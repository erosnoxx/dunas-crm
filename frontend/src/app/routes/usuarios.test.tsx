import { QueryClientProvider } from "@tanstack/react-query"
import { render, screen, within } from "@testing-library/react"
import { createRoutesStub } from "react-router"
import { beforeEach, describe, expect, it, vi } from "vitest"

import * as service from "@/features/iam/iam.service"
import type { CurrentUser, Role } from "@/features/iam/iam.service"
import { createQueryClient } from "@/shared/lib/query-client"

import Authenticated from "./authenticated"
import Home from "./home"
import Usuarios from "./usuarios"
import UsuariosId from "./usuarios.$id"

vi.mock("@/features/iam/iam.service")

const me = (role: Role): CurrentUser => ({
  id: "1",
  name: "Marcos Silva",
  email: "marcos@dunas.com",
  role,
  mustChangePassword: false,
})

function renderAt(path: string, role: Role) {
  vi.mocked(service.getCurrentUser).mockResolvedValue(me(role))
  vi.mocked(service.listUsers).mockResolvedValue({
    content: [
      {
        id: "7",
        name: "Paula Lima",
        email: "paula@dunas.com",
        role: "COMMERCIAL",
        status: "ACTIVE",
        lastUsedAt: new Date().toISOString(),
      },
    ],
    page: 0,
    size: 5,
    totalElements: 1,
    totalPages: 1,
  })
  const client = createQueryClient(() => undefined)
  const Stub = createRoutesStub([
    {
      Component: () => (
        <QueryClientProvider client={client}>
          <Authenticated />
        </QueryClientProvider>
      ),
      children: [
        { path: "/", Component: Home },
        {
          path: "/usuarios",
          Component: Usuarios,
          children: [{ path: ":id", Component: UsuariosId }],
        },
      ],
    },
  ])

  render(<Stub initialEntries={[path]} />)
}

beforeEach(() => vi.resetAllMocks())

describe("rota /usuarios", () => {
  it("o Administrador abre a lista dentro da casca, com o título na barra do celular e o item marcado", async () => {
    renderAt("/usuarios", "ADMINISTRATOR")

    expect(await screen.findByText("Paula Lima")).toBeInTheDocument()
    const nav = screen.getByRole("navigation", { name: "Principal" })
    expect(within(nav).getByRole("link", { name: "Usuários" })).toHaveAttribute(
      "aria-current",
      "page",
    )
    expect(within(screen.getByRole("banner")).getByText("Usuários")).toBeInTheDocument()
  })

  it.each<Role>(["COMMERCIAL", "TECHNICIAN"])(
    "%s que força a URL volta para a página inicial, sem pedir a lista",
    async (role) => {
      renderAt("/usuarios", role)

      expect(await screen.findByText("Em construção")).toBeInTheDocument()
      expect(service.listUsers).not.toHaveBeenCalled()
      expect(screen.queryByText("Paula Lima")).not.toBeInTheDocument()
    },
  )

  it("o detalhe de /usuarios/:id também é só do Administrador", async () => {
    renderAt("/usuarios/7", "COMMERCIAL")

    expect(await screen.findByText("Em construção")).toBeInTheDocument()
    expect(service.listUsers).not.toHaveBeenCalled()
  })
})

describe("rota /usuarios/:id", () => {
  it("mostra a lista e a região de detalhe (o encaixe do painel) sem barra superior no celular", async () => {
    renderAt("/usuarios/7", "ADMINISTRATOR")

    expect(await screen.findByText("Paula Lima")).toBeInTheDocument()
    expect(screen.getByRole("region", { name: "Detalhe do usuário" })).toBeInTheDocument()
    // O celular tem cabeçalho próprio no detalhe: a casca não põe a barra superior.
    expect(screen.queryByRole("banner")).not.toBeInTheDocument()
  })
})
