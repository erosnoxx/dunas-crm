import { QueryClientProvider } from "@tanstack/react-query"
import { act, render, screen, waitFor, within } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import { createRoutesStub } from "react-router"
import { beforeEach, describe, expect, it, vi } from "vitest"

import * as service from "@/features/iam/iam.service"
import type { CurrentUser, Role } from "@/features/iam/iam.service"
import { createQueryClient } from "@/shared/lib/query-client"

import Authenticated from "../routes/authenticated"
import ContaTrocarSenha from "../routes/conta.trocar-senha"
import Home from "../routes/home"

vi.mock("@/features/iam/iam.service")
vi.mock("sonner", () => ({ toast: { success: vi.fn() } }))

const me = (role: Role, mustChangePassword = false): CurrentUser => ({
  id: "1",
  name: "Marcos Silva",
  email: "marcos@dunas.com",
  role,
  mustChangePassword,
})

function renderAt(path: string, user: CurrentUser) {
  vi.mocked(service.getCurrentUser).mockResolvedValue(user)
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
        { path: "/usuarios", Component: () => <p>tela de usuários</p> },
        { path: "/conta/sessoes", Component: () => <p>tela de sessões</p> },
        { path: "/conta/trocar-senha", Component: ContaTrocarSenha },
      ],
    },
    { path: "/entrar", Component: () => <p>tela de entrar</p> },
  ])

  render(<Stub initialEntries={[path]} />)

  return userEvent.setup()
}

const sidebar = () => screen.getByRole("navigation", { name: "Principal" })
const trigger = () => screen.getByRole("button", { name: "Minha conta, Marcos Silva" })

beforeEach(() => vi.resetAllMocks())

describe("barra lateral", () => {
  it("o Administrador vê o item Usuários, com o atual marcado", async () => {
    renderAt("/usuarios", me("ADMINISTRATOR"))

    const item = await within(await findSidebar()).findByRole("link", {
      name: "Usuários",
    })

    expect(item).toHaveAttribute("href", "/usuarios")
    expect(item).toHaveAttribute("aria-current", "page")
  })

  it.each<Role>(["COMMERCIAL", "TECHNICIAN"])("%s não vê Usuários", async (role) => {
    renderAt("/", me(role))

    const nav = await findSidebar()

    expect(within(nav).queryByRole("link", { name: "Usuários" })).not.toBeInTheDocument()
    expect(within(nav).getByRole("link", { name: "Minhas sessões" })).toBeInTheDocument()
  })

  it("o bloco da conta mostra a inicial, o nome e o papel de me", async () => {
    renderAt("/", me("TECHNICIAN"))

    const nav = await findSidebar()

    expect(within(nav).getByText("M")).toBeInTheDocument()
    expect(within(nav).getByText("Marcos Silva")).toBeInTheDocument()
    expect(within(nav).getByText("Técnico")).toBeInTheDocument()
  })

  it("tem Minhas sessões, Trocar senha e Sair, e o item da página atual é marcado", async () => {
    renderAt("/conta/sessoes", me("ADMINISTRATOR"))

    const nav = await findSidebar()

    expect(within(nav).getByRole("link", { name: "Minhas sessões" })).toHaveAttribute(
      "href",
      "/conta/sessoes",
    )
    expect(within(nav).getByRole("link", { name: "Minhas sessões" })).toHaveAttribute(
      "aria-current",
      "page",
    )
    // Mockup "Minhas sessões · web": o item atual da conta é texto claro e seminegrito.
    expect(within(nav).getByRole("link", { name: "Minhas sessões" })).toHaveClass(
      "aria-[current=page]:text-text",
      "aria-[current=page]:font-semibold",
    )
    expect(within(nav).getByRole("link", { name: "Trocar senha" })).toHaveAttribute(
      "href",
      "/conta/trocar-senha",
    )
    expect(within(nav).getByRole("button", { name: "Sair" })).toBeInTheDocument()
  })

  it("Sair encerra a sessão sem confirmar e vai para o login", async () => {
    vi.mocked(service.logout).mockResolvedValue(undefined)
    const user = renderAt("/", me("COMMERCIAL"))

    await user.click(within(await findSidebar()).getByRole("button", { name: "Sair" }))

    expect(service.logout).toHaveBeenCalledTimes(1)
    expect(await screen.findByText("tela de entrar")).toBeInTheDocument()
  })

  it("o teclado alcança todos os itens, do primeiro ao Sair", async () => {
    const user = renderAt("/", me("ADMINISTRATOR"))
    const nav = await findSidebar()
    const order = ["Usuários", "Minhas sessões", "Trocar senha", "Sair"]

    // O avatar da barra superior vem antes na ordem do documento.
    await user.tab()
    expect(trigger()).toHaveFocus()
    for (const name of order) {
      await user.tab()
      expect(
        within(nav).getByRole(name === "Sair" ? "button" : "link", { name }),
      ).toHaveFocus()
    }
  })
})

describe("menu da conta no celular", () => {
  it("abre pelo teclado com as três ações, e Esc fecha devolvendo o foco ao avatar", async () => {
    const user = renderAt("/usuarios", me("ADMINISTRATOR"))
    await findSidebar()

    trigger().focus()
    await user.keyboard("{Enter}")
    const dialog = await screen.findByRole("dialog", { name: "Minha conta" })

    expect(within(dialog).getByText("Marcos Silva")).toBeInTheDocument()
    expect(within(dialog).getByText("Administrador")).toBeInTheDocument()
    expect(within(dialog).getByRole("link", { name: "Minhas sessões" })).toHaveAttribute(
      "href",
      "/conta/sessoes",
    )
    expect(within(dialog).getByRole("link", { name: "Trocar senha" })).toHaveAttribute(
      "href",
      "/conta/trocar-senha",
    )
    expect(within(dialog).getByRole("button", { name: "Sair" })).toBeInTheDocument()

    await user.keyboard("{Escape}")

    await waitFor(() => expect(screen.queryByRole("dialog")).not.toBeInTheDocument())
    expect(trigger()).toHaveFocus()
  })

  it("o botão Fechar fecha o menu", async () => {
    const user = renderAt("/usuarios", me("ADMINISTRATOR"))
    await findSidebar()

    await user.click(trigger())
    const dialog = await screen.findByRole("dialog", { name: "Minha conta" })
    await user.click(within(dialog).getByRole("button", { name: "Fechar" }))

    await waitFor(() => expect(screen.queryByRole("dialog")).not.toBeInTheDocument())
  })

  it("escolher uma página fecha o menu e navega", async () => {
    const user = renderAt("/usuarios", me("COMMERCIAL"))
    await findSidebar()

    await user.click(trigger())
    const dialog = await screen.findByRole("dialog", { name: "Minha conta" })
    await user.click(within(dialog).getByRole("link", { name: "Minhas sessões" }))

    expect(await screen.findByText("tela de sessões")).toBeInTheDocument()
    await waitFor(() => expect(screen.queryByRole("dialog")).not.toBeInTheDocument())
  })

  it("fecha a folha aberta quando a tela cruza o breakpoint md", async () => {
    const original = window.matchMedia
    const listeners = new Set<(event: { matches: boolean }) => void>()
    window.matchMedia = ((query: string) => ({
      matches: false,
      media: query,
      addEventListener: (_: string, fn: (event: { matches: boolean }) => void) =>
        listeners.add(fn),
      removeEventListener: (_: string, fn: (event: { matches: boolean }) => void) =>
        listeners.delete(fn),
    })) as unknown as typeof window.matchMedia

    try {
      const user = renderAt("/usuarios", me("ADMINISTRATOR"))
      await findSidebar()

      await user.click(trigger())
      await screen.findByRole("dialog", { name: "Minha conta" })

      act(() => listeners.forEach((fn) => fn({ matches: true })))

      await waitFor(() => expect(screen.queryByRole("dialog")).not.toBeInTheDocument())
    } finally {
      window.matchMedia = original
    }
  })

  it("Sair no menu encerra a sessão sem confirmar e vai para o login", async () => {
    vi.mocked(service.logout).mockResolvedValue(undefined)
    const user = renderAt("/usuarios", me("TECHNICIAN"))
    await findSidebar()

    await user.click(trigger())
    const dialog = await screen.findByRole("dialog", { name: "Minha conta" })
    await user.click(within(dialog).getByRole("button", { name: "Sair" }))

    expect(service.logout).toHaveBeenCalledTimes(1)
    expect(await screen.findByText("tela de entrar")).toBeInTheDocument()
  })
})

describe("troca de senha", () => {
  it("a obrigatória não ganha casca: sem navegação e sem menu da conta", async () => {
    renderAt("/conta/trocar-senha", me("ADMINISTRATOR", true))

    await screen.findByRole("heading", { name: "Troque a senha inicial" })

    expect(screen.queryByRole("navigation")).not.toBeInTheDocument()
    expect(screen.queryByRole("button", { name: /Minha conta/ })).not.toBeInTheDocument()
  })

  it("Minhas sessões fica na casca, sem a barra superior: o cabeçalho próprio tem o Voltar", async () => {
    renderAt("/conta/sessoes", me("COMMERCIAL"))

    await findSidebar()

    expect(screen.queryByRole("button", { name: /Minha conta/ })).not.toBeInTheDocument()
  })

  it("a voluntária fica na casca, sem a barra superior que dobraria o cabeçalho dela", async () => {
    renderAt("/conta/trocar-senha", me("COMMERCIAL"))

    await screen.findByRole("heading", { level: 1, name: "Trocar senha" })

    expect(sidebar()).toBeInTheDocument()
    expect(screen.getByRole("link", { name: "Voltar" })).toBeInTheDocument()
    expect(screen.queryByRole("button", { name: /Minha conta/ })).not.toBeInTheDocument()
  })
})

const findSidebar = () => screen.findByRole("navigation", { name: "Principal" })
