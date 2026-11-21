import { QueryClientProvider } from "@tanstack/react-query"
import { render, screen } from "@testing-library/react"
import { createRoutesStub, Outlet } from "react-router"
import { beforeEach, describe, expect, it, vi } from "vitest"

import * as service from "@/features/iam/iam.service"
import { createQueryClient } from "@/shared/lib/query-client"

import Entrar from "./entrar"

vi.mock("@/features/iam/iam.service")

function renderAt(path: string) {
  const client = createQueryClient(() => undefined)
  const Stub = createRoutesStub([
    {
      Component: () => (
        <QueryClientProvider client={client}>
          <Outlet />
        </QueryClientProvider>
      ),
      children: [
        { path: "/entrar", Component: Entrar },
        { path: "/", Component: () => <p>início</p> },
      ],
    },
  ])

  render(<Stub initialEntries={[path]} />)
}

beforeEach(() => vi.resetAllMocks())

describe("rota /entrar", () => {
  it("sem sessão mostra a tela de entrar", async () => {
    vi.mocked(service.getCurrentUser).mockRejectedValue({
      status: 401,
      code: "session_invalid",
      message: "sessão",
    })

    renderAt("/entrar")

    expect(await screen.findByRole("heading", { name: "Entrar" })).toBeInTheDocument()
  })

  it("com sessão vai para o início", async () => {
    vi.mocked(service.getCurrentUser).mockResolvedValue({
      id: "1",
      name: "Ana",
      email: "a@b.com",
      role: "ADMINISTRATOR",
      mustChangePassword: false,
    })

    renderAt("/entrar")

    expect(await screen.findByText("início")).toBeInTheDocument()
  })
})
