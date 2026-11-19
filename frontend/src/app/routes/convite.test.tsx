import { QueryClientProvider } from "@tanstack/react-query"
import { render, screen } from "@testing-library/react"
import { createRoutesStub, Outlet } from "react-router"
import { beforeEach, describe, expect, it, vi } from "vitest"

import * as service from "@/features/iam/iam.service"
import { createQueryClient } from "@/shared/lib/query-client"

import Convite from "./convite"

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
      children: [{ path: "/convite", Component: Convite }],
    },
  ])

  render(<Stub initialEntries={[path]} />)
}

beforeEach(() => vi.resetAllMocks())

describe("rota /convite", () => {
  it("com token mostra a tela de criar a senha, sem consultar a sessão nem o token", async () => {
    renderAt("/convite?token=abc")

    expect(
      await screen.findByRole("heading", { name: "Crie sua senha" }),
    ).toBeInTheDocument()
    expect(service.getCurrentUser).not.toHaveBeenCalled()
    expect(service.acceptInvitation).not.toHaveBeenCalled()
  })

  it("sem token mostra o aviso de link usado", async () => {
    renderAt("/convite")

    expect(
      await screen.findByRole("heading", {
        name: "Este convite já foi usado ou foi substituído",
      }),
    ).toBeInTheDocument()
  })
})
