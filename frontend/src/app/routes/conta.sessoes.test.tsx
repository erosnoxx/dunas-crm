import { QueryClientProvider } from "@tanstack/react-query"
import { render, screen, within } from "@testing-library/react"
import { createRoutesStub } from "react-router"
import { beforeEach, describe, expect, it, vi } from "vitest"

import * as service from "@/features/iam/iam.service"
import type { CurrentUser } from "@/features/iam/iam.service"
import { createQueryClient } from "@/shared/lib/query-client"

import Authenticated from "./authenticated"
import ContaSessoes from "./conta.sessoes"

vi.mock("@/features/iam/iam.service")

const me: CurrentUser = {
  id: "7",
  name: "Rafael Souza",
  email: "rafael@dunas.com",
  role: "TECHNICIAN",
  mustChangePassword: false,
}

beforeEach(() => vi.resetAllMocks())

describe("rota /conta/sessoes", () => {
  it("abre dentro da casca autenticada, com o título na barra do celular e o item marcado", async () => {
    vi.mocked(service.getCurrentUser).mockResolvedValue(me)
    vi.mocked(service.listSessions).mockResolvedValue([
      {
        id: "s1",
        device: "Chrome, Windows",
        startedAt: new Date().toISOString(),
        lastUsedAt: new Date().toISOString(),
        current: true,
      },
    ])
    const client = createQueryClient(() => undefined)
    const Stub = createRoutesStub([
      {
        Component: () => (
          <QueryClientProvider client={client}>
            <Authenticated />
          </QueryClientProvider>
        ),
        children: [{ path: "/conta/sessoes", Component: ContaSessoes }],
      },
    ])

    render(<Stub initialEntries={["/conta/sessoes"]} />)

    expect(await screen.findByText("Chrome, Windows")).toBeInTheDocument()
    const nav = screen.getByRole("navigation", { name: "Principal" })
    expect(within(nav).getByRole("link", { name: "Minhas sessões" })).toHaveAttribute(
      "aria-current",
      "page",
    )
    expect(screen.getAllByText("Minhas sessões").length).toBeGreaterThanOrEqual(2)
  })
})
