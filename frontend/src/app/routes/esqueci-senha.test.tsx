import { QueryClientProvider } from "@tanstack/react-query"
import { render, screen } from "@testing-library/react"
import { createRoutesStub, Outlet } from "react-router"
import { beforeEach, describe, expect, it, vi } from "vitest"

import * as service from "@/features/iam/iam.service"
import { createQueryClient } from "@/shared/lib/query-client"

import EsqueciSenha from "./esqueci-senha"
import RedefinirSenha from "./redefinir-senha"

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
        { path: "/esqueci-senha", Component: EsqueciSenha },
        { path: "/redefinir-senha", Component: RedefinirSenha },
      ],
    },
  ])

  render(<Stub initialEntries={[path]} />)
}

beforeEach(() => vi.resetAllMocks())

describe("rotas públicas de senha", () => {
  it("/esqueci-senha mostra o pedido do link, sem consultar a sessão", async () => {
    renderAt("/esqueci-senha")

    expect(
      await screen.findByRole("heading", { name: "Esqueceu a senha?" }),
    ).toBeInTheDocument()
    expect(service.getCurrentUser).not.toHaveBeenCalled()
  })

  it("/redefinir-senha?token= mostra a nova senha, sem consultar a sessão", async () => {
    renderAt("/redefinir-senha?token=abc")

    expect(
      await screen.findByRole("heading", { name: "Crie uma nova senha" }),
    ).toBeInTheDocument()
    expect(service.getCurrentUser).not.toHaveBeenCalled()
  })

  it("/redefinir-senha sem token cai no link usado", async () => {
    renderAt("/redefinir-senha")

    expect(
      await screen.findByRole("heading", {
        name: "Este link já foi usado ou foi substituído",
      }),
    ).toBeInTheDocument()
  })
})
