import { QueryClient, QueryClientProvider } from "@tanstack/react-query"
import { renderHook, waitFor } from "@testing-library/react"
import type { ReactNode } from "react"
import { beforeEach, describe, expect, it, vi } from "vitest"

import { queryClient as appQueryClient } from "@/shared/lib/query-client"

import {
  useChangeUserRole,
  useCurrentUser,
  useLogin,
  useRevokeSession,
} from "./iam.hooks"
import * as service from "./iam.service"

vi.mock("./iam.service")

function setup() {
  const client = new QueryClient({
    defaultOptions: {
      ...appQueryClient.getDefaultOptions(),
      queries: { ...appQueryClient.getDefaultOptions().queries, retryDelay: 0 },
    },
  })
  const wrapper = ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={client}>{children}</QueryClientProvider>
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
