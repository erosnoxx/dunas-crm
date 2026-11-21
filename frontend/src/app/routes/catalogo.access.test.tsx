import { QueryClientProvider } from "@tanstack/react-query"
import { renderHook, waitFor } from "@testing-library/react"
import type { ReactNode } from "react"
import { beforeEach, describe, expect, it, vi } from "vitest"

import * as service from "@/features/iam/iam.service"
import type { CurrentUser, Role } from "@/features/iam/iam.service"
import { createQueryClient } from "@/shared/lib/query-client"

import { useCanEditCatalog } from "./catalogo.access"

vi.mock("@/features/iam/iam.service")

const me = (role: Role): CurrentUser => ({
  id: "1",
  name: "Marcos Silva",
  email: "marcos@dunas.com",
  role,
  mustChangePassword: false,
})

function renderCanEdit() {
  const client = createQueryClient(() => undefined)
  const wrapper = ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={client}>{children}</QueryClientProvider>
  )

  return renderHook(() => useCanEditCatalog(), { wrapper })
}

beforeEach(() => vi.resetAllMocks())

describe("useCanEditCatalog", () => {
  it.each<Role>(["ADMINISTRATOR", "COMMERCIAL"])("%s edita o catálogo", async (role) => {
    vi.mocked(service.getCurrentUser).mockResolvedValue(me(role))

    const { result } = renderCanEdit()

    await waitFor(() => expect(result.current).toBe(true))
  })

  it("o Técnico só consulta", async () => {
    vi.mocked(service.getCurrentUser).mockResolvedValue(me("TECHNICIAN"))

    const { result } = renderCanEdit()

    await waitFor(() => expect(service.getCurrentUser).toHaveBeenCalled())
    await waitFor(() => expect(result.current).toBe(false))
  })

  it("sem usuário carregado, não edita", () => {
    vi.mocked(service.getCurrentUser).mockReturnValue(new Promise(() => undefined))

    const { result } = renderCanEdit()

    expect(result.current).toBe(false)
  })
})
