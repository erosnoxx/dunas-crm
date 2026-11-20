import { QueryClientProvider } from "@tanstack/react-query"
import { render, screen, waitFor, within } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import { createRoutesStub, Outlet } from "react-router"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"

import { createQueryClient } from "@/shared/lib/query-client"

import * as service from "../iam.service"
import type { CurrentUser, SessionSummary } from "../iam.service"
import { SessionsScreen } from "./sessions"

vi.mock("../iam.service")

// Só o `Date` é congelado: os timers reais seguem para o TanStack Query e o user-event.
const NOW = new Date(2026, 10, 19, 12, 0, 0)
const ago = (ms: number) => new Date(NOW.getTime() - ms).toISOString()
const MINUTE = 60_000
const DAY = 24 * 60 * MINUTE
const day = (month: number, date: number) => new Date(2026, month, date, 9).toISOString()

const me: CurrentUser = {
  id: "7",
  name: "Rafael Souza",
  email: "rafael@dunas.com",
  role: "TECHNICIAN",
  mustChangePassword: false,
}

const CURRENT: SessionSummary = {
  id: "s1",
  device: "Galaxy A54, app DUNAS",
  startedAt: day(10, 3),
  lastUsedAt: ago(30_000),
  current: true,
}
const IPHONE: SessionSummary = {
  id: "s2",
  device: "iPhone, Safari",
  startedAt: day(10, 12),
  lastUsedAt: ago(2 * DAY),
  current: false,
}
const CHROME: SessionSummary = {
  id: "s3",
  device: "Chrome, Windows",
  startedAt: day(10, 10),
  lastUsedAt: ago(9 * DAY),
  current: false,
}

function renderScreen() {
  const client = createQueryClient(() => undefined)
  const Stub = createRoutesStub([
    {
      Component: () => (
        <QueryClientProvider client={client}>
          <Outlet />
        </QueryClientProvider>
      ),
      children: [
        { path: "/conta/sessoes", Component: SessionsScreen },
        { path: "/entrar", Component: () => <p>tela de entrar</p> },
      ],
    },
  ])

  render(<Stub initialEntries={["/conta/sessoes"]} />)

  return userEvent.setup()
}

const item = (device: string) => screen.getByText(device).closest("li") as HTMLElement
const endButton = (device: string) =>
  within(item(device)).queryByRole("button", { name: "Encerrar esta sessão" })

beforeEach(() => {
  vi.resetAllMocks()
  vi.useFakeTimers({ toFake: ["Date"] })
  vi.setSystemTime(NOW)
  vi.mocked(service.getCurrentUser).mockResolvedValue(me)
  vi.mocked(service.listSessions).mockResolvedValue([CURRENT, IPHONE, CHROME])
})

afterEach(() => vi.useRealTimers())

describe("lista", () => {
  it("pede as sessões do próprio usuário e mostra aparelho, último uso e entrada", async () => {
    renderScreen()

    expect(await screen.findByText("Galaxy A54, app DUNAS")).toBeInTheDocument()
    expect(service.listSessions).toHaveBeenCalledWith("7")
    expect(screen.getAllByRole("listitem")).toHaveLength(3)
    expect(
      within(item("Galaxy A54, app DUNAS")).getByText("Último uso: agora"),
    ).toBeVisible()
    expect(
      within(item("Galaxy A54, app DUNAS")).getByText("Entrou em 03/11"),
    ).toBeVisible()
    expect(
      within(item("iPhone, Safari")).getByText("Último uso: há 2 dias"),
    ).toBeVisible()
    expect(within(item("iPhone, Safari")).getByText("Entrou em 12/11")).toBeVisible()
    expect(
      within(item("Chrome, Windows")).getByText("Último uso: há 9 dias"),
    ).toBeVisible()
    expect(within(item("Chrome, Windows")).getByText("Entrou em 10/11")).toBeVisible()
  })

  it("o título da página e o texto fixo da duração da sessão", async () => {
    renderScreen()

    expect(
      await screen.findByRole("heading", { level: 1, name: "Minhas sessões" }),
    ).toHaveClass("max-md:sr-only")
    expect(screen.getByText("A sessão dura 30 dias e renova a cada uso.")).toBeVisible()
    expect(
      screen.getByText(
        "Aparelhos conectados à sua conta. Encerre os que você não reconhece.",
      ),
    ).toBeVisible()
  })

  it("a sessão do aparelho tem o chip Esta sessão (círculo cheio) e nenhum botão de encerrar", async () => {
    renderScreen()
    await screen.findByText("Galaxy A54, app DUNAS")

    const chip = within(item("Galaxy A54, app DUNAS")).getByText("Esta sessão")

    expect(chip.querySelector("svg")).toHaveAttribute("data-shape", "filled")
    expect(endButton("Galaxy A54, app DUNAS")).not.toBeInTheDocument()
    expect(
      within(item("iPhone, Safari")).queryByText("Esta sessão"),
    ).not.toBeInTheDocument()
  })

  it("as outras sessões têm Encerrar esta sessão e o ícone do aparelho", async () => {
    renderScreen()
    await screen.findByText("Galaxy A54, app DUNAS")

    expect(endButton("iPhone, Safari")).toBeInTheDocument()
    expect(endButton("Chrome, Windows")).toBeInTheDocument()
    expect(item("iPhone, Safari").querySelector("svg.lucide-smartphone")).not.toBeNull()
    expect(item("Chrome, Windows").querySelector("svg.lucide-monitor")).not.toBeNull()
  })
})

describe("encerrar uma sessão", () => {
  it("chama revokeSession sem confirmação e tira a sessão da lista", async () => {
    vi.mocked(service.revokeSession).mockResolvedValue(undefined)
    const user = renderScreen()
    await screen.findByText("Galaxy A54, app DUNAS")
    vi.mocked(service.listSessions).mockResolvedValue([CURRENT, CHROME])

    await user.click(endButton("iPhone, Safari")!)

    expect(vi.mocked(service.revokeSession).mock.calls[0]?.[0]).toBe("s2")
    expect(screen.queryByRole("dialog")).not.toBeInTheDocument()
    await waitFor(() =>
      expect(screen.queryByText("iPhone, Safari")).not.toBeInTheDocument(),
    )
    expect(screen.getAllByRole("listitem")).toHaveLength(2)
  })

  it("Encerrar todas as outras chama revokeOtherSessions e deixa só a atual", async () => {
    vi.mocked(service.revokeOtherSessions).mockResolvedValue(undefined)
    const user = renderScreen()
    await screen.findByText("Galaxy A54, app DUNAS")
    vi.mocked(service.listSessions).mockResolvedValue([CURRENT])

    await user.click(screen.getByRole("button", { name: "Encerrar todas as outras" }))

    expect(vi.mocked(service.revokeOtherSessions).mock.calls[0]?.[0]).toBe("7")
    await waitFor(() => expect(screen.getAllByRole("listitem")).toHaveLength(1))
    expect(screen.getByText("Galaxy A54, app DUNAS")).toBeInTheDocument()
  })

  it("sem outras sessões, Encerrar todas as outras fica desligado", async () => {
    vi.mocked(service.listSessions).mockResolvedValue([CURRENT])
    renderScreen()
    await screen.findByText("Galaxy A54, app DUNAS")

    expect(
      screen.getByRole("button", { name: "Encerrar todas as outras" }),
    ).toBeDisabled()
  })
})

describe("Sair deste aparelho", () => {
  it("encerra a sessão e vai para o login", async () => {
    vi.mocked(service.logout).mockResolvedValue(undefined)
    const user = renderScreen()
    await screen.findByText("Galaxy A54, app DUNAS")

    await user.click(screen.getByRole("button", { name: "Sair deste aparelho" }))

    expect(service.logout).toHaveBeenCalledTimes(1)
    expect(await screen.findByText("tela de entrar")).toBeInTheDocument()
  })
})

describe("estados", () => {
  it("enquanto carrega mostra o indicador de carga, sem lista", async () => {
    vi.mocked(service.listSessions).mockReturnValue(new Promise(() => undefined))
    renderScreen()

    expect(await screen.findByRole("status")).toHaveAttribute("aria-busy", "true")
    expect(screen.queryByRole("list")).not.toBeInTheDocument()
  })

  it("sem sessões não desenha a lista", async () => {
    vi.mocked(service.listSessions).mockResolvedValue([])
    renderScreen()

    await screen.findByRole("heading", { level: 1, name: "Minhas sessões" })
    await waitFor(() => expect(screen.queryByRole("status")).not.toBeInTheDocument())

    expect(screen.queryByRole("list")).not.toBeInTheDocument()
    expect(
      screen.getByRole("button", { name: "Encerrar todas as outras" }),
    ).toBeDisabled()
  })

  it("falha de rede ao carregar mostra o aviso", async () => {
    vi.mocked(service.listSessions).mockRejectedValue({
      status: 0,
      message: "não foi possível conectar ao servidor",
    })
    renderScreen()

    // Falha de rede repete uma vez (`shouldRetry`), com a espera padrão de 1 s.
    expect(await screen.findByRole("alert", {}, { timeout: 4000 })).toHaveTextContent(
      "Não foi possível conectar ao servidor",
    )
    expect(screen.queryByRole("list")).not.toBeInTheDocument()
  })

  it("falha de rede ao encerrar mantém a lista e mostra o aviso", async () => {
    vi.mocked(service.revokeSession).mockRejectedValue({
      status: 0,
      message: "não foi possível conectar ao servidor",
    })
    const user = renderScreen()
    await screen.findByText("Galaxy A54, app DUNAS")

    await user.click(endButton("iPhone, Safari")!)

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Não foi possível conectar ao servidor",
    )
    expect(screen.getAllByRole("listitem")).toHaveLength(3)
    expect(endButton("iPhone, Safari")).toBeEnabled()
  })

  it("decide a mensagem pelo code: sessão que já não existe", async () => {
    vi.mocked(service.revokeSession).mockRejectedValue({
      status: 404,
      code: "session_not_found",
      message: "texto da API",
    })
    const user = renderScreen()
    await screen.findByText("Galaxy A54, app DUNAS")

    await user.click(endButton("iPhone, Safari")!)

    expect(await screen.findByRole("alert")).toHaveTextContent("Sessão não encontrada")
  })
})
