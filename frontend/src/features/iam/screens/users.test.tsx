import { QueryClientProvider } from "@tanstack/react-query"
import { render, screen, waitFor, within } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import { createRoutesStub, Outlet, useParams } from "react-router"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"

import { createQueryClient } from "@/shared/lib/query-client"

import * as service from "../iam.service"
import type { CurrentUser, UserSummary } from "../iam.service"
import { UsersScreen } from "./users"

vi.mock("../iam.service")

// Só o `Date` é congelado: os timers reais seguem para o TanStack Query e o user-event.
const NOW = new Date(2026, 10, 19, 12, 0, 0)
const ago = (ms: number) => new Date(NOW.getTime() - ms).toISOString()
const MINUTE = 60_000
const HOUR = 60 * MINUTE
const DAY = 24 * HOUR

const me: CurrentUser = {
  id: "u1",
  name: "Marcos Silva",
  email: "marcos@exemplo.com.br",
  role: "ADMINISTRATOR",
  mustChangePassword: false,
}

const user = (n: number, over: Partial<UserSummary> = {}): UserSummary => ({
  id: `u${n}`,
  name: `Usuário ${n}`,
  email: `u${n}@exemplo.com.br`,
  role: "TECHNICIAN",
  status: "ACTIVE",
  lastUsedAt: ago(2 * MINUTE),
  ...over,
})

// As cinco primeiras linhas são as dos mockups; o resto completa os 54 do rodapé.
const FIRST_FIVE: UserSummary[] = [
  user(1, {
    name: "Marcos Silva",
    email: "marcos@exemplo.com.br",
    role: "ADMINISTRATOR",
    lastUsedAt: ago(10_000),
  }),
  user(2, {
    name: "Paula Lima",
    email: "paula@exemplo.com.br",
    role: "COMMERCIAL",
    lastUsedAt: ago(3 * HOUR),
  }),
  user(3, { name: "Rafael Souza", email: "rafael@exemplo.com.br" }),
  user(4, {
    name: "Tiago Alves",
    email: "tiago@exemplo.com.br",
    status: "INVITED",
    lastUsedAt: null,
  }),
  user(5, {
    name: "Carlos Pinto",
    email: "carlos@exemplo.com.br",
    status: "DEACTIVATED",
    lastUsedAt: ago(40 * DAY),
  }),
]
const ALL = [...FIRST_FIVE, ...Array.from({ length: 49 }, (_, i) => user(i + 6))]

function serve(all: UserSummary[]) {
  vi.mocked(service.listUsers).mockImplementation(async ({ page, size }) => ({
    content: all.slice(page * size, (page + 1) * size),
    page,
    size,
    totalElements: all.length,
    totalPages: Math.ceil(all.length / size),
  }))
}

function setDesktop(matches: boolean) {
  window.matchMedia = ((query: string) => ({
    matches,
    media: query,
    addEventListener: () => undefined,
    removeEventListener: () => undefined,
  })) as unknown as typeof window.matchMedia
}

function Detail() {
  const { id } = useParams()

  return <p>detalhe de {id}</p>
}

function renderScreen(path = "/usuarios") {
  const client = createQueryClient(() => undefined)
  const Stub = createRoutesStub([
    {
      Component: () => (
        <QueryClientProvider client={client}>
          <Outlet />
        </QueryClientProvider>
      ),
      children: [
        {
          path: "/usuarios",
          Component: UsersScreen,
          children: [{ path: ":id", Component: Detail }],
        },
      ],
    },
  ])

  render(<Stub initialEntries={[path]} />)

  return userEvent.setup()
}

const originalMatchMedia = window.matchMedia
const row = (name: string) => screen.getByText(name).closest("li") as HTMLElement

beforeEach(() => {
  vi.resetAllMocks()
  vi.useFakeTimers({ toFake: ["Date"] })
  vi.setSystemTime(NOW)
  vi.mocked(service.getCurrentUser).mockResolvedValue(me)
  serve(ALL)
})

afterEach(() => {
  vi.useRealTimers()
  window.matchMedia = originalMatchMedia
})

describe("linhas (celular)", () => {
  it("pede a primeira página de 5 e mostra uma linha por usuário, como link para o detalhe", async () => {
    renderScreen()

    expect(await screen.findByText("Paula Lima")).toBeInTheDocument()
    expect(service.listUsers).toHaveBeenCalledWith({ page: 0, size: 5 })
    expect(screen.getAllByRole("listitem")).toHaveLength(5)
    expect(within(row("Paula Lima")).getByRole("link")).toHaveAttribute(
      "href",
      "/usuarios/u2",
    )
  })

  it("mostra papel e último uso em texto relativo, com a hora do teste", async () => {
    renderScreen()
    await screen.findByText("Paula Lima")

    expect(within(row("Marcos Silva (você)")).getByText("Administrador")).toBeVisible()
    expect(within(row("Marcos Silva (você)")).getByText("agora")).toBeVisible()
    expect(within(row("Paula Lima")).getByText("Comercial")).toBeVisible()
    expect(within(row("Paula Lima")).getByText("há 3 horas")).toBeVisible()
    expect(within(row("Rafael Souza")).getByText("há 2 minutos")).toBeVisible()
    expect(within(row("Tiago Alves")).getByText("nunca entrou")).toBeVisible()
    expect(within(row("Carlos Pinto")).getByText("há 40 dias")).toBeVisible()
  })

  it("cada situação tem o texto e uma forma diferente: cheio, anel e cortado", async () => {
    renderScreen()
    await screen.findByText("Paula Lima")

    const shape = (name: string, label: string) =>
      within(row(name)).getByText(label).querySelector("svg")

    expect(shape("Paula Lima", "Ativo")).toHaveAttribute("data-shape", "filled")
    expect(shape("Tiago Alves", "Convite pendente")).toHaveAttribute("data-shape", "ring")
    expect(shape("Carlos Pinto", "Desativado")).toHaveAttribute("data-shape", "slashed")
  })

  it('põe "(você)" só ao lado do próprio usuário', async () => {
    renderScreen()
    await screen.findByText("Paula Lima")

    expect(screen.getByText("Marcos Silva (você)")).toBeInTheDocument()
    expect(screen.getAllByText(/\(você\)/)).toHaveLength(1)
  })

  it("o título é só para leitor de tela no celular (a barra da casca mostra o título)", async () => {
    renderScreen()

    const heading = await screen.findByRole("heading", { level: 1, name: "Usuários" })

    expect(heading).toHaveClass("text-display-lg", "max-md:sr-only")
  })

  it("Convidar usuário é um link para a futura rota do convite", async () => {
    renderScreen()

    expect(await screen.findByRole("link", { name: "Convidar usuário" })).toHaveAttribute(
      "href",
      "/usuarios/convidar",
    )
  })

  it("clicar numa linha navega para /usuarios/:id e a lista some no celular", async () => {
    const user = renderScreen()
    await screen.findByText("Paula Lima")

    await user.click(within(row("Paula Lima")).getByRole("link"))

    expect(await screen.findByText("detalhe de u2")).toBeInTheDocument()
    expect(screen.getByRole("region", { name: "Lista de usuários" })).toHaveClass(
      "max-md:hidden",
    )
  })
})

describe("paginação no celular", () => {
  it('mostra "Mostrando 5 de 54 usuários" e Carregar mais acrescenta a próxima página', async () => {
    const user = renderScreen()
    await screen.findByText("Paula Lima")

    expect(screen.getByText("Mostrando 5 de 54 usuários")).toBeVisible()
    expect(screen.queryByRole("button", { name: "Anterior" })).not.toBeInTheDocument()

    await user.click(screen.getByRole("button", { name: "Carregar mais" }))

    expect(await screen.findByText("Usuário 6")).toBeInTheDocument()
    expect(service.listUsers).toHaveBeenLastCalledWith({ page: 1, size: 5 })
    expect(screen.getByText("Paula Lima")).toBeInTheDocument()
    expect(screen.getAllByRole("listitem")).toHaveLength(10)
    expect(screen.getByText("Mostrando 10 de 54 usuários")).toBeVisible()
  })

  it("Carregar mais fica desligado quando todos já foram carregados", async () => {
    serve(ALL.slice(0, 7))
    const user = renderScreen()
    await screen.findByText("Paula Lima")

    await user.click(screen.getByRole("button", { name: "Carregar mais" }))
    await screen.findByText("Usuário 7")

    expect(screen.getByText("Mostrando 7 de 7 usuários")).toBeVisible()
    expect(screen.getByRole("button", { name: "Carregar mais" })).toBeDisabled()
  })
})

describe("paginação na web", () => {
  beforeEach(() => setDesktop(true))

  it('mostra "Mostrando 1 a 5 de 54 usuários", com Anterior desligado na primeira página', async () => {
    renderScreen()
    await screen.findByText("Paula Lima")

    expect(screen.getByText("Mostrando 1 a 5 de 54 usuários")).toBeVisible()
    expect(screen.getByRole("button", { name: "Anterior" })).toBeDisabled()
    expect(screen.getByRole("button", { name: "Próxima" })).toBeEnabled()
    expect(
      screen.queryByRole("button", { name: "Carregar mais" }),
    ).not.toBeInTheDocument()
  })

  it("Próxima e Anterior trocam de página, e Próxima desliga na última (4 linhas)", async () => {
    const user = renderScreen()
    await screen.findByText("Paula Lima")

    await user.click(screen.getByRole("button", { name: "Próxima" }))

    expect(await screen.findByText("Usuário 6")).toBeInTheDocument()
    expect(service.listUsers).toHaveBeenLastCalledWith({ page: 1, size: 5 })
    expect(screen.queryByText("Paula Lima")).not.toBeInTheDocument()
    expect(screen.getByText("Mostrando 6 a 10 de 54 usuários")).toBeVisible()
    expect(screen.getByRole("button", { name: "Anterior" })).toBeEnabled()

    await user.click(screen.getByRole("button", { name: "Anterior" }))
    expect(await screen.findByText("Paula Lima")).toBeInTheDocument()

    for (let n = 0; n < 10; n++) {
      await waitFor(() =>
        expect(screen.getByRole("button", { name: "Próxima" })).toBeEnabled(),
      )
      await user.click(screen.getByRole("button", { name: "Próxima" }))
    }

    expect(await screen.findByText("Mostrando 51 a 54 de 54 usuários")).toBeVisible()
    expect(screen.getByRole("button", { name: "Próxima" })).toBeDisabled()
  })

  it("mostra as colunas e o e-mail de cada usuário", async () => {
    renderScreen()
    await screen.findByText("Paula Lima")

    for (const column of ["Usuário", "Papel", "Situação", "Último uso"])
      expect(screen.getByText(column)).toBeVisible()
    expect(within(row("Paula Lima")).getByText("paula@exemplo.com.br")).toBeVisible()
  })

  it("com /usuarios/:id a linha selecionada leva o fundo line e o detalhe fica ao lado", async () => {
    renderScreen("/usuarios/u3")

    expect(await screen.findByText("Rafael Souza")).toBeInTheDocument()
    expect(screen.getByText("detalhe de u3")).toBeInTheDocument()
    const selected = within(row("Rafael Souza")).getByRole("link")

    expect(selected).toHaveAttribute("aria-current", "page")
    expect(selected.firstElementChild).toHaveClass("group-aria-[current=page]:bg-line")
    expect(within(row("Paula Lima")).getByRole("link")).not.toHaveAttribute(
      "aria-current",
    )
    expect(screen.getByRole("region", { name: "Lista de usuários" })).toBeVisible()
  })

  it("sem usuário selecionado não há região de detalhe", async () => {
    renderScreen()
    await screen.findByText("Paula Lima")

    expect(screen.queryByText(/detalhe de/)).not.toBeInTheDocument()
  })
})

describe("estados", () => {
  it("carregando mostra o indicador e nenhuma linha", async () => {
    vi.mocked(service.listUsers).mockReturnValue(new Promise(() => undefined))
    renderScreen()

    expect(await screen.findByRole("status")).toHaveAttribute("aria-busy", "true")
    expect(screen.queryAllByRole("listitem")).toHaveLength(0)
  })

  it("403 do ListUsers mostra a mensagem de permissão e nenhuma lista", async () => {
    vi.mocked(service.listUsers).mockRejectedValue({
      status: 403,
      code: "forbidden",
      message: "Forbidden",
    })
    renderScreen()

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Você não tem permissão para isto",
    )
    expect(screen.queryAllByRole("listitem")).toHaveLength(0)
    expect(screen.queryByText(/Mostrando/)).not.toBeInTheDocument()
  })

  it("erro de rede tem aviso próprio", async () => {
    vi.mocked(service.listUsers).mockRejectedValue({
      status: 0,
      message: "não foi possível conectar ao servidor",
    })
    renderScreen()

    expect(await screen.findByRole("alert", {}, { timeout: 4000 })).toHaveTextContent(
      "Não foi possível conectar ao servidor",
    )
    expect(screen.queryAllByRole("listitem")).toHaveLength(0)
  })

  it("lista vazia tem estado próprio, sem rodapé de paginação", async () => {
    serve([])
    renderScreen()

    expect(await screen.findByText("Nenhum usuário encontrado")).toBeVisible()
    expect(screen.queryAllByRole("listitem")).toHaveLength(0)
    expect(screen.queryByText(/Mostrando/)).not.toBeInTheDocument()
  })
})
