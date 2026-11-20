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
import UsuariosConvidar from "./usuarios.convidar"
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
          children: [
            { path: "convidar", Component: UsuariosConvidar },
            { path: ":id", Component: UsuariosId },
          ],
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

const PAULA_ID = "77777777-7777-4777-8777-777777777777"

describe("rota /usuarios/:id", () => {
  it("mostra a lista e o detalhe do usuário, sem barra superior no celular", async () => {
    vi.mocked(service.getUser).mockResolvedValue({
      user: {
        id: PAULA_ID,
        name: "Paula Lima",
        email: "paula@dunas.com",
        role: "COMMERCIAL",
        status: "ACTIVE",
        lastUsedAt: null,
      },
      sessions: [],
    })
    renderAt(`/usuarios/${PAULA_ID}`, "ADMINISTRATOR")

    expect(
      await screen.findByRole("button", { name: "Desativar usuário" }),
    ).toBeInTheDocument()
    expect(service.getUser).toHaveBeenCalledWith(PAULA_ID)
    expect(screen.getByRole("region", { name: "Lista de usuários" })).toBeInTheDocument()
    // O celular tem cabeçalho próprio no detalhe: a casca não põe a barra superior.
    expect(screen.queryByRole("button", { name: /Minha conta/ })).not.toBeInTheDocument()
  })
})

describe("rota /usuarios/convidar", () => {
  it("é a rota do convite, não um detalhe: não consulta usuário e a casca não põe a barra superior no celular", async () => {
    renderAt("/usuarios/convidar", "ADMINISTRATOR")

    expect(await screen.findByLabelText("Nome completo")).toBeInTheDocument()
    expect(service.getUser).not.toHaveBeenCalled()
    expect(screen.queryByRole("button", { name: /Minha conta/ })).not.toBeInTheDocument()
    // No celular a lista dá lugar à tela do convite.
    expect(screen.getByRole("region", { name: "Lista de usuários" })).toHaveClass(
      "max-md:hidden",
    )
  })

  it("na web, a ligação direta mostra o convite como diálogo sobre a lista", async () => {
    const original = window.matchMedia
    window.matchMedia = ((query: string) => ({
      matches: true,
      media: query,
      addEventListener: () => undefined,
      removeEventListener: () => undefined,
    })) as unknown as typeof window.matchMedia

    try {
      renderAt("/usuarios/convidar", "ADMINISTRATOR")

      expect(
        await screen.findByRole("dialog", { name: "Convidar usuário" }),
      ).toBeVisible()
      // A lista segue atrás do diálogo (modal: fora da árvore de acessibilidade, mas no DOM).
      expect(await screen.findByText("Paula Lima")).toBeInTheDocument()
      expect(service.getUser).not.toHaveBeenCalled()
    } finally {
      window.matchMedia = original
    }
  })

  it("só o Administrador chega", async () => {
    renderAt("/usuarios/convidar", "TECHNICIAN")

    expect(await screen.findByText("Em construção")).toBeInTheDocument()
  })
})
