import { QueryClientProvider } from "@tanstack/react-query"
import { renderHook, waitFor } from "@testing-library/react"
import type { ReactNode } from "react"
import { MemoryRouter, useLocation } from "react-router"
import { beforeEach, describe, expect, it, vi } from "vitest"

import { createQueryClient } from "@/shared/lib/query-client"

import {
  useChangeUserRole,
  useCurrentUser,
  useLogin,
  useLogout,
  useRevokeSession,
} from "./iam.hooks"
import * as service from "./iam.service"

vi.mock("./iam.service")

function setup() {
  const client = createQueryClient(() => undefined)

  client.setDefaultOptions({
    ...client.getDefaultOptions(),
    queries: { ...client.getDefaultOptions().queries, retryDelay: 0 },
  })
  const wrapper = ({ children }: { children: ReactNode }) => (
    <MemoryRouter initialEntries={["/painel"]}>
      <QueryClientProvider client={client}>{children}</QueryClientProvider>
    </MemoryRouter>
  )

  return { client, wrapper }
}

beforeEach(() => vi.resetAllMocks())

describe("useCurrentUser", () => {
  it("não tenta de novo em 401", async () => {
    vi.mocked(service.getCurrentUser).mockRejectedValue({ status: 401, message: "x" })
    const { wrapper } = setup()

    const { result } = renderHook(() => useCurrentUser(), { wrapper })

    await waitFor(() => expect(result.current.isError).toBe(true))
    expect(service.getCurrentUser).toHaveBeenCalledTimes(1)
  })

  it("guarda o resultado na chave me", async () => {
    const me = {
      id: "1",
      name: "Ana",
      email: "a@b.com",
      role: "ADMINISTRATOR",
      mustChangePassword: false,
    } as const
    vi.mocked(service.getCurrentUser).mockResolvedValue(me)
    const { client, wrapper } = setup()

    const { result } = renderHook(() => useCurrentUser(), { wrapper })

    await waitFor(() => expect(result.current.data).toEqual(me))
    expect(client.getQueryData(["me"])).toEqual(me)
    expect(client.getQueryState(["me"])?.isInvalidated).toBe(false)
  })
})

describe("mutations", () => {
  it("login invalida me", async () => {
    vi.mocked(service.login).mockResolvedValue({
      expiresAt: "x",
      mustChangePassword: false,
    })
    const { client, wrapper } = setup()
    const invalidate = vi.spyOn(client, "invalidateQueries")

    const { result } = renderHook(() => useLogin(), { wrapper })
    await result.current.mutateAsync({ email: "a@b.com", password: "x" })

    expect(invalidate).toHaveBeenCalledWith({ queryKey: ["me"] })
  })

  it("changeUserRole invalida users", async () => {
    vi.mocked(service.changeUserRole).mockResolvedValue(undefined)
    const { client, wrapper } = setup()
    const invalidate = vi.spyOn(client, "invalidateQueries")

    const { result } = renderHook(() => useChangeUserRole(), { wrapper })
    await result.current.mutateAsync({ id: "u1", role: "TECHNICIAN" })

    expect(service.changeUserRole).toHaveBeenCalledWith("u1", "TECHNICIAN")
    expect(invalidate).toHaveBeenCalledWith({ queryKey: ["users"] })
  })

  it("revokeSession invalida sessions e users", async () => {
    vi.mocked(service.revokeSession).mockResolvedValue(undefined)
    const { client, wrapper } = setup()
    const invalidate = vi.spyOn(client, "invalidateQueries")

    const { result } = renderHook(() => useRevokeSession(), { wrapper })
    await result.current.mutateAsync("s1")

    expect(invalidate).toHaveBeenCalledWith({ queryKey: ["sessions"] })
    expect(invalidate).toHaveBeenCalledWith({ queryKey: ["users"] })
  })
})

describe("useLogout", () => {
  it("encerra a sessão, limpa o cache inteiro e vai para o login", async () => {
    vi.mocked(service.logout).mockResolvedValue(undefined)
    const { client, wrapper } = setup()
    client.setQueryData(["me"], { id: "1" })
    client.setQueryData(["users", { page: 0, size: 10 }], [])

    const { result } = renderHook(
      () => ({ logout: useLogout(), path: useLocation().pathname }),
      {
        wrapper,
      },
    )
    await result.current.logout.mutateAsync()

    expect(service.logout).toHaveBeenCalledTimes(1)
    expect(client.getQueryCache().getAll()).toHaveLength(0)
    await waitFor(() => expect(result.current.path).toBe("/entrar"))
  })

  it("se o logout falha, mantém o cache e fica onde está", async () => {
    vi.mocked(service.logout).mockRejectedValue({ status: 0, message: "rede" })
    const { client, wrapper } = setup()
    client.setQueryData(["me"], { id: "1" })

    const { result } = renderHook(
      () => ({ logout: useLogout(), path: useLocation().pathname }),
      {
        wrapper,
      },
    )
    await result.current.logout.mutateAsync().catch(() => undefined)

    expect(client.getQueryData(["me"])).toEqual({ id: "1" })
    expect(result.current.path).toBe("/painel")
  })
})
