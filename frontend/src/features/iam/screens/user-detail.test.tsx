import { QueryClientProvider } from "@tanstack/react-query"
import { act, render, screen, waitFor, within } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import { createRoutesStub, Outlet } from "react-router"
import { toast } from "sonner"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"

import { createQueryClient } from "@/shared/lib/query-client"

import * as service from "../iam.service"
import type { CurrentUser, SessionSummary, UserDetail, UserSummary } from "../iam.service"
import { UsersScreen } from "./users"
import { UserDetailScreen } from "./user-detail"

vi.mock("../iam.service")
vi.mock("sonner", () => ({ toast: { success: vi.fn() } }))

// O TanStack Query passa o contexto da mutation como 2º argumento de `mutationFn`: o 1º é o que importa.
// Só o `Date` é congelado: os timers reais seguem para o TanStack Query e o user-event.
const NOW = new Date(2026, 10, 19, 12, 0, 0)
const ago = (ms: number) => new Date(NOW.getTime() - ms).toISOString()
const MINUTE = 60_000
const DAY = 24 * 60 * MINUTE

const ME_ID = "11111111-1111-4111-8111-111111111111"
const RAFAEL_ID = "33333333-3333-4333-8333-333333333333"
const TIAGO_ID = "44444444-4444-4444-8444-444444444444"
const CARLOS_ID = "55555555-5555-4555-8555-555555555555"

const me: CurrentUser = {
  id: ME_ID,
  name: "Marcos Silva",
  email: "marcos@exemplo.com.br",
  role: "ADMINISTRATOR",
  mustChangePassword: false,
}

const rafael: UserSummary = {
  id: RAFAEL_ID,
  name: "Rafael Souza",
  email: "rafael@exemplo.com.br",
  role: "TECHNICIAN",
  status: "ACTIVE",
  lastUsedAt: ago(2 * MINUTE),
}
const tiago: UserSummary = {
  id: TIAGO_ID,
  name: "Tiago Alves",
  email: "tiago@exemplo.com.br",
  role: "TECHNICIAN",
  status: "INVITED",
  lastUsedAt: null,
}
const carlos: UserSummary = {
  id: CARLOS_ID,
  name: "Carlos Pinto",
  email: "carlos@exemplo.com.br",
  role: "TECHNICIAN",
  status: "DEACTIVATED",
  lastUsedAt: ago(40 * DAY),
}
const marcos: UserSummary = {
  id: ME_ID,
  name: "Marcos Silva",
  email: "marcos@exemplo.com.br",
  role: "ADMINISTRATOR",
  status: "ACTIVE",
  lastUsedAt: ago(10_000),
}

const session = (id: string, device: string, lastUsedAt: string, current = false) =>
  ({
    id,
    device,
    startedAt: ago(5 * DAY),
    lastUsedAt,
    current,
  }) satisfies SessionSummary

const GALAXY = session("s1", "Galaxy A54, app DUNAS", ago(2 * MINUTE))
const CHROME = session("s2", "Chrome, Windows", ago(3 * DAY))

const detail = (user: UserSummary, sessions: SessionSummary[] = []): UserDetail => ({
  user,
  sessions,
})

function setDesktop(matches: boolean) {
  window.matchMedia = ((query: string) => ({
    matches,
    media: query,
    addEventListener: () => undefined,
    removeEventListener: () => undefined,
  })) as unknown as typeof window.matchMedia
}

function renderAt(id: string) {
  const client = createQueryClient(() => undefined)
  const Stub = createRoutesStub([
    {
      Component: () => (
        <QueryClientProvider client={client}>
          <Outlet />
        </QueryClientProvider>
      ),
      children: [
        { path: "/usuarios", Component: () => <p>lista de usuários</p> },
        { path: "/usuarios/:id", Component: UserDetailScreen },
      ],
    },
  ])

  render(<Stub initialEntries={[`/usuarios/${id}`]} />)

  return userEvent.setup()
}

const originalMatchMedia = window.matchMedia
const button = (name: string | RegExp) => screen.getByRole("button", { name })
const noButton = (name: string | RegExp) =>
  expect(screen.queryByRole("button", { name })).not.toBeInTheDocument()

beforeEach(() => {
  vi.resetAllMocks()
  vi.useFakeTimers({ toFake: ["Date"] })
  vi.setSystemTime(NOW)
  setDesktop(true)
  vi.mocked(service.getCurrentUser).mockResolvedValue(me)
})

afterEach(() => {
  vi.useRealTimers()
  window.matchMedia = originalMatchMedia
})

describe("usuário ativo (web)", () => {
  beforeEach(() => {
    vi.mocked(service.getUser).mockResolvedValue(detail(rafael, [GALAXY, CHROME]))
  })

  it("mostra só as ações dele: sessões, papel e desativar", async () => {
    renderAt(RAFAEL_ID)

    expect(
      await screen.findByRole("heading", { level: 2, name: "Rafael Souza" }),
    ).toBeVisible()
    expect(screen.getByText("rafael@exemplo.com.br")).toBeVisible()
    expect(
      screen.getByRole("heading", { level: 3, name: "Sessões ativas" }),
    ).toBeVisible()
    expect(screen.getAllByRole("button", { name: "Encerrar" })).toHaveLength(2)
    expect(button("Encerrar todas as sessões")).toBeEnabled()
    expect(screen.getByRole("combobox", { name: "Papel" })).toHaveValue("TECHNICIAN")
    expect(button("Desativar usuário")).toBeVisible()
    noButton("Reativar usuário")
    noButton("Reenviar convite")
  })

  it("pede o usuário pelo id da rota", async () => {
    renderAt(RAFAEL_ID)
    await screen.findByText("Rafael Souza")

    expect(service.getUser).toHaveBeenCalledWith(RAFAEL_ID)
  })

  it("cada sessão mostra o aparelho e o último uso, com a hora do teste", async () => {
    renderAt(RAFAEL_ID)
    await screen.findByText("Rafael Souza")

    const first = screen.getByText("Galaxy A54, app DUNAS").closest("li") as HTMLElement
    const second = screen.getByText("Chrome, Windows").closest("li") as HTMLElement

    expect(within(first).getByText("Último uso: há 2 min")).toBeVisible()
    expect(within(second).getByText("Último uso: há 3 dias")).toBeVisible()
    expect(screen.getByText("2")).toBeVisible()
  })

  it("não há h1 no painel da web (o h1 é o da lista)", async () => {
    renderAt(RAFAEL_ID)
    await screen.findByText("Rafael Souza")

    expect(screen.queryByRole("heading", { level: 1 })).not.toBeInTheDocument()
  })

  it("Encerrar chama revokeSession sem confirmação e atualiza o painel", async () => {
    vi.mocked(service.revokeSession).mockResolvedValue()
    vi.mocked(service.getUser)
      .mockResolvedValueOnce(detail(rafael, [GALAXY, CHROME]))
      .mockResolvedValue(detail(rafael, [CHROME]))
    const user = renderAt(RAFAEL_ID)
    await screen.findByText("Galaxy A54, app DUNAS")

    const li = screen.getByText("Galaxy A54, app DUNAS").closest("li") as HTMLElement
    await user.click(within(li).getByRole("button", { name: "Encerrar" }))

    expect(screen.queryByRole("dialog")).not.toBeInTheDocument()
    expect(service.revokeSession).toHaveBeenCalledWith("s1", expect.anything())
    await waitFor(() =>
      expect(screen.queryByText("Galaxy A54, app DUNAS")).not.toBeInTheDocument(),
    )
    expect(screen.getByText("Chrome, Windows")).toBeVisible()
  })

  it("Encerrar todas as sessões chama revokeAllSessions com o usuário aberto", async () => {
    vi.mocked(service.revokeAllSessions).mockResolvedValue()
    vi.mocked(service.getUser)
      .mockResolvedValueOnce(detail(rafael, [GALAXY, CHROME]))
      .mockResolvedValue(detail(rafael, []))
    const user = renderAt(RAFAEL_ID)
    await screen.findByText("Rafael Souza")

    await user.click(button("Encerrar todas as sessões"))

    expect(service.revokeAllSessions).toHaveBeenCalledWith(RAFAEL_ID, expect.anything())
    await waitFor(() =>
      expect(screen.queryByText("Chrome, Windows")).not.toBeInTheDocument(),
    )
  })

  it("sem sessões, Encerrar todas fica desligado", async () => {
    vi.mocked(service.getUser).mockResolvedValue(detail(rafael, []))
    renderAt(RAFAEL_ID)
    await screen.findByText("Rafael Souza")

    expect(button("Encerrar todas as sessões")).toBeDisabled()
  })

  it("trocar o papel chama changeUserRole e o painel mostra o novo", async () => {
    vi.mocked(service.changeUserRole).mockResolvedValue()
    vi.mocked(service.getUser)
      .mockResolvedValueOnce(detail(rafael, [GALAXY]))
      .mockResolvedValue(detail({ ...rafael, role: "COMMERCIAL" }, [GALAXY]))
    const user = renderAt(RAFAEL_ID)
    await screen.findByText("Rafael Souza")

    await user.selectOptions(
      screen.getByRole("combobox", { name: "Papel" }),
      "COMMERCIAL",
    )

    expect(service.changeUserRole).toHaveBeenCalledWith(RAFAEL_ID, "COMMERCIAL")
    await waitFor(() =>
      expect(screen.getByRole("combobox", { name: "Papel" })).toHaveValue("COMMERCIAL"),
    )
  })

  it("Desativar usuário chama deactivateUser sem confirmação e o painel vira o de desativado", async () => {
    vi.mocked(service.deactivateUser).mockResolvedValue()
    vi.mocked(service.getUser)
      .mockResolvedValueOnce(detail(rafael, [GALAXY]))
      .mockResolvedValue(detail({ ...rafael, status: "DEACTIVATED" }))
    const user = renderAt(RAFAEL_ID)
    await screen.findByText("Rafael Souza")

    await user.click(button("Desativar usuário"))

    expect(screen.queryByRole("dialog")).not.toBeInTheDocument()
    expect(service.deactivateUser).toHaveBeenCalledWith(RAFAEL_ID, expect.anything())
    expect(await screen.findByRole("button", { name: "Reativar usuário" })).toBeVisible()
    noButton("Desativar usuário")
  })

  it("o botão fica em carregamento e não dispara duas vezes", async () => {
    let finish: () => void = () => undefined
    vi.mocked(service.deactivateUser).mockReturnValue(
      new Promise<void>((resolve) => (finish = resolve)),
    )
    const user = renderAt(RAFAEL_ID)
    await screen.findByText("Rafael Souza")

    await user.dblClick(button("Desativar usuário"))

    expect(button("Desativar usuário")).toHaveAttribute("aria-busy", "true")
    expect(service.deactivateUser).toHaveBeenCalledTimes(1)
    await act(async () => finish())
  })

  it("Encerrar em carregamento só marca a sessão clicada", async () => {
    vi.mocked(service.revokeSession).mockReturnValue(new Promise<void>(() => undefined))
    const user = renderAt(RAFAEL_ID)
    await screen.findByText("Rafael Souza")

    const [first, second] = screen.getAllByRole("button", { name: "Encerrar" })
    await user.click(first)

    expect(first).toHaveAttribute("aria-busy", "true")
    expect(second).not.toHaveAttribute("aria-busy")
    await user.click(first)
    expect(service.revokeSession).toHaveBeenCalledTimes(1)
  })

  it.each([
    ["cannot_change_own_role", "Você não pode mudar o seu próprio papel"],
    ["user_not_active", "Este usuário não está ativo"],
  ])(
    "erro %s da troca de papel sai como aviso, e o papel volta ao do servidor",
    async (code, text) => {
      vi.mocked(service.changeUserRole).mockRejectedValue({
        status: 422,
        code,
        message: "x",
      })
      const user = renderAt(RAFAEL_ID)
      await screen.findByText("Rafael Souza")

      await user.selectOptions(
        screen.getByRole("combobox", { name: "Papel" }),
        "COMMERCIAL",
      )

      expect(await screen.findByRole("alert")).toHaveTextContent(text)
      expect(screen.getByRole("combobox", { name: "Papel" })).toHaveValue("TECHNICIAN")
    },
  )

  it("cannot_deactivate_self do servidor sai como aviso", async () => {
    vi.mocked(service.deactivateUser).mockRejectedValue({
      status: 422,
      code: "cannot_deactivate_self",
      message: "x",
    })
    const user = renderAt(RAFAEL_ID)
    await screen.findByText("Rafael Souza")

    await user.click(button("Desativar usuário"))

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Você não pode desativar a sua própria conta",
    )
  })

  it("o aviso de uma ação some quando outra começa", async () => {
    vi.mocked(service.deactivateUser).mockRejectedValue({
      status: 422,
      code: "user_not_active",
      message: "x",
    })
    vi.mocked(service.revokeAllSessions).mockResolvedValue()
    const user = renderAt(RAFAEL_ID)
    await screen.findByText("Rafael Souza")
    await user.click(button("Desativar usuário"))
    await screen.findByRole("alert")

    await user.click(button("Encerrar todas as sessões"))

    await waitFor(() => expect(screen.queryByRole("alert")).not.toBeInTheDocument())
  })
})

describe("usuário desativado", () => {
  beforeEach(() => {
    vi.mocked(service.getUser).mockResolvedValue(detail(carlos))
  })

  it("mostra o texto aprovado e só Reativar usuário (web)", async () => {
    renderAt(CARLOS_ID)

    expect(
      await screen.findByRole("heading", { level: 2, name: "Carlos Pinto" }),
    ).toBeVisible()
    expect(screen.getByText("carlos@exemplo.com.br, Técnico")).toBeVisible()
    expect(screen.getByText("Desativado")).toBeVisible()
    expect(
      screen.getByText(
        "As sessões foram encerradas e este usuário não consegue entrar. Ao reativar, ele volta a entrar com a senha que já tem.",
      ),
    ).toBeVisible()
    expect(button("Reativar usuário")).toBeVisible()
    noButton("Desativar usuário")
    noButton(/Encerrar/)
    expect(screen.queryByRole("combobox")).not.toBeInTheDocument()
    expect(screen.queryByText("Sessões ativas")).not.toBeInTheDocument()
  })

  it("Reativar chama reactivateUser sem confirmação e o painel volta a ativo", async () => {
    vi.mocked(service.reactivateUser).mockResolvedValue()
    vi.mocked(service.getUser)
      .mockResolvedValueOnce(detail(carlos))
      .mockResolvedValue(detail({ ...carlos, status: "ACTIVE" }, []))
    const user = renderAt(CARLOS_ID)
    await screen.findByText("Carlos Pinto")

    await user.click(button("Reativar usuário"))

    expect(screen.queryByRole("dialog")).not.toBeInTheDocument()
    expect(service.reactivateUser).toHaveBeenCalledWith(CARLOS_ID, expect.anything())
    expect(await screen.findByRole("button", { name: "Desativar usuário" })).toBeVisible()
  })

  it("celular: h1 com o nome, Voltar, cartão Sem acesso e Reativar no rodapé", async () => {
    setDesktop(false)
    renderAt(CARLOS_ID)

    expect(
      await screen.findByRole("heading", { level: 1, name: "Carlos Pinto" }),
    ).toBeVisible()
    expect(
      screen.queryByRole("heading", { level: 2, name: "Carlos Pinto" }),
    ).not.toBeInTheDocument()
    expect(screen.getByRole("link", { name: "Voltar" })).toHaveAttribute(
      "href",
      "/usuarios",
    )
    expect(screen.getByRole("heading", { level: 2, name: "Sem acesso" })).toBeVisible()
    expect(screen.getByText("Papel")).toBeVisible()
    expect(button("Reativar usuário")).toBeVisible()
  })
})

describe("convite pendente", () => {
  beforeEach(() => {
    vi.mocked(service.getUser).mockResolvedValue(detail(tiago))
  })

  it("web: texto aprovado, Reenviar convite e Desativar usuário, sem seletor de papel", async () => {
    renderAt(TIAGO_ID)

    expect(
      await screen.findByRole("heading", { level: 2, name: "Tiago Alves" }),
    ).toBeVisible()
    expect(screen.getByText("tiago@exemplo.com.br, Técnico")).toBeVisible()
    expect(screen.getByText("Convite pendente")).toBeVisible()
    expect(
      screen.getByText(
        "Tiago ainda não definiu a senha. Reenviar o convite invalida o link anterior.",
      ),
    ).toBeVisible()
    expect(button("Reenviar convite")).toBeVisible()
    expect(button("Desativar usuário")).toBeVisible()
    expect(screen.queryByRole("combobox")).not.toBeInTheDocument()
    noButton(/Encerrar/)
    noButton("Reativar usuário")
  })

  it("Reenviar convite chama resendInvitation e mostra o aviso aprovado", async () => {
    vi.mocked(service.resendInvitation).mockResolvedValue()
    const user = renderAt(TIAGO_ID)
    await screen.findByText("Tiago Alves")

    await user.click(button("Reenviar convite"))

    expect(service.resendInvitation).toHaveBeenCalledWith(TIAGO_ID, expect.anything())
    await waitFor(() => expect(toast.success).toHaveBeenCalledWith("Convite reenviado"))
  })

  it("Reenviar convite fica em carregamento e não dispara duas vezes", async () => {
    vi.mocked(service.resendInvitation).mockReturnValue(
      new Promise<void>(() => undefined),
    )
    const user = renderAt(TIAGO_ID)
    await screen.findByText("Tiago Alves")

    await user.dblClick(button("Reenviar convite"))

    expect(button("Reenviar convite")).toHaveAttribute("aria-busy", "true")
    expect(service.resendInvitation).toHaveBeenCalledTimes(1)
  })

  it("convite que já foi aceito sai como aviso (user_not_invited)", async () => {
    vi.mocked(service.resendInvitation).mockRejectedValue({
      status: 422,
      code: "user_not_invited",
      message: "x",
    })
    const user = renderAt(TIAGO_ID)
    await screen.findByText("Tiago Alves")

    await user.click(button("Reenviar convite"))

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Este usuário não está com convite pendente",
    )
    expect(toast.success).not.toHaveBeenCalled()
  })

  it("celular: cartão Convite não aceito, Papel só em texto e Reenviar no rodapé", async () => {
    setDesktop(false)
    renderAt(TIAGO_ID)

    expect(
      await screen.findByRole("heading", { level: 1, name: "Tiago Alves" }),
    ).toBeVisible()
    expect(
      screen.getByRole("heading", { level: 2, name: "Convite não aceito" }),
    ).toBeVisible()
    expect(screen.getByText("Técnico")).toBeVisible()
    expect(screen.queryByRole("combobox")).not.toBeInTheDocument()
    expect(button("Reenviar convite")).toBeVisible()
    expect(button("Desativar usuário")).toBeVisible()
  })

  it("Desativar usuário no convite pendente chama deactivateUser", async () => {
    vi.mocked(service.deactivateUser).mockResolvedValue()
    const user = renderAt(TIAGO_ID)
    await screen.findByText("Tiago Alves")

    await user.click(button("Desativar usuário"))

    expect(service.deactivateUser).toHaveBeenCalledWith(TIAGO_ID, expect.anything())
  })
})

describe("o próprio usuário", () => {
  const own = (sessions: SessionSummary[]) => detail(marcos, sessions)
  const HERE = session("s9", "Chrome, Windows", ago(10_000), true)
  const PHONE = session("s8", "iPhone, Safari", ago(2 * DAY))

  it("web: sem seletor de papel e sem Desativar, com a frase aprovada", async () => {
    vi.mocked(service.getUser).mockResolvedValue(own([HERE, PHONE]))
    renderAt(ME_ID)

    expect(
      await screen.findByRole("heading", { level: 2, name: "Marcos Silva (você)" }),
    ).toBeVisible()
    expect(screen.getByText("marcos@exemplo.com.br, Administrador")).toBeVisible()
    expect(screen.getByText("Ativo")).toBeVisible()
    expect(
      screen.getByText("Você não pode mudar o seu papel nem desativar a sua conta."),
    ).toBeVisible()
    expect(screen.queryByRole("combobox")).not.toBeInTheDocument()
    noButton("Desativar usuário")
    noButton("Reativar usuário")
    noButton("Reenviar convite")
  })

  it("a sessão do aparelho é marcada e não tem Encerrar; a outra tem", async () => {
    vi.mocked(service.getUser).mockResolvedValue(own([HERE, PHONE]))
    renderAt(ME_ID)
    await screen.findByText("Marcos Silva (você)")

    const here = screen.getByText("Chrome, Windows").closest("li") as HTMLElement
    const other = screen.getByText("iPhone, Safari").closest("li") as HTMLElement

    expect(within(here).getByText("Esta sessão")).toBeVisible()
    expect(within(here).queryByRole("button")).not.toBeInTheDocument()
    expect(
      within(other).getByRole("button", { name: "Encerrar esta sessão" }),
    ).toBeVisible()
  })

  it("Encerrar todas as outras chama revokeOtherSessions, não revokeAllSessions", async () => {
    vi.mocked(service.getUser).mockResolvedValue(own([HERE, PHONE]))
    vi.mocked(service.revokeOtherSessions).mockResolvedValue()
    const user = renderAt(ME_ID)
    await screen.findByText("Marcos Silva (você)")

    await user.click(button("Encerrar todas as outras"))

    expect(service.revokeOtherSessions).toHaveBeenCalledWith(ME_ID, expect.anything())
    expect(service.revokeAllSessions).not.toHaveBeenCalled()
    noButton("Encerrar todas as sessões")
  })

  it("celular: o Papel é texto, com a frase aprovada, e não há Desativar", async () => {
    setDesktop(false)
    vi.mocked(service.getUser).mockResolvedValue(own([HERE, PHONE]))
    renderAt(ME_ID)

    expect(
      await screen.findByRole("heading", { level: 1, name: "Marcos Silva (você)" }),
    ).toBeVisible()
    expect(screen.getByText("Administrador")).toBeVisible()
    expect(
      screen.getByText("Você não pode mudar o seu papel nem desativar a sua conta."),
    ).toBeVisible()
    expect(screen.queryByRole("combobox")).not.toBeInTheDocument()
    noButton("Desativar usuário")
  })

  it("vale também quando a linha clicada na lista é o próprio usuário", async () => {
    vi.mocked(service.listUsers).mockResolvedValue({
      content: [marcos, rafael],
      page: 0,
      size: 5,
      totalElements: 2,
      totalPages: 1,
    })
    vi.mocked(service.getUser).mockImplementation(async (id) =>
      id === ME_ID ? own([HERE]) : detail(rafael, [GALAXY]),
    )
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
            children: [{ path: ":id", Component: UserDetailScreen }],
          },
        ],
      },
    ])
    render(<Stub initialEntries={["/usuarios"]} />)
    const user = userEvent.setup()

    await user.click(await screen.findByRole("link", { name: /Marcos Silva \(você\)/ }))
    await screen.findByRole("heading", { level: 2, name: "Marcos Silva (você)" })

    expect(screen.queryByRole("combobox")).not.toBeInTheDocument()
    noButton("Desativar usuário")

    await user.click(screen.getByRole("link", { name: /Rafael Souza/ }))
    expect(await screen.findByRole("combobox", { name: "Papel" })).toBeVisible()
    expect(button("Desativar usuário")).toBeVisible()
  })

  it("depois de uma ação, a linha da lista do selecionado atualiza o chip", async () => {
    let status: UserSummary["status"] = "ACTIVE"
    vi.mocked(service.listUsers).mockImplementation(async () => ({
      content: [marcos, { ...rafael, status }],
      page: 0,
      size: 5,
      totalElements: 2,
      totalPages: 1,
    }))
    vi.mocked(service.getUser).mockImplementation(async () =>
      detail({ ...rafael, status }, [GALAXY]),
    )
    vi.mocked(service.deactivateUser).mockImplementation(async () => {
      status = "DEACTIVATED"
    })
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
            children: [{ path: ":id", Component: UserDetailScreen }],
          },
        ],
      },
    ])
    render(<Stub initialEntries={[`/usuarios/${RAFAEL_ID}`]} />)
    const user = userEvent.setup()
    const row = async () =>
      (await screen.findByRole("link", { name: /Rafael Souza/ })) as HTMLElement

    expect(within(await row()).getByText("Ativo")).toBeVisible()
    await user.click(await screen.findByRole("button", { name: "Desativar usuário" }))

    await waitFor(async () =>
      expect(within(await row()).getByText("Desativado")).toBeVisible(),
    )
  })
})

describe("erros e id inválido", () => {
  it("id que não é UUID não consulta o servidor e mostra usuário não encontrado", async () => {
    renderAt("convidar-x")

    expect(await screen.findByRole("alert")).toHaveTextContent("Usuário não encontrado")
    expect(service.getUser).not.toHaveBeenCalled()
  })

  it.each([
    ["user_not_found", 404, "Usuário não encontrado"],
    ["forbidden", 403, "Você não tem permissão para isto"],
  ])("%s mostra a mensagem da tabela de erros", async (code, status, text) => {
    vi.mocked(service.getUser).mockRejectedValue({ status, code, message: "x" })
    renderAt(RAFAEL_ID)

    expect(await screen.findByRole("alert")).toHaveTextContent(text)
    noButton("Desativar usuário")
  })

  it("falha de rede mostra a mensagem da API", async () => {
    vi.mocked(service.getUser).mockRejectedValue({
      status: 0,
      message: "não foi possível conectar ao servidor",
    })
    renderAt(RAFAEL_ID)

    // Falha de rede repete uma vez (1 s) antes de mostrar o erro.
    expect(await screen.findByRole("alert", {}, { timeout: 4000 })).toHaveTextContent(
      "Não foi possível conectar ao servidor",
    )
  })

  it("enquanto carrega, mostra o estado de carregamento", async () => {
    vi.mocked(service.getUser).mockReturnValue(new Promise<UserDetail>(() => undefined))
    renderAt(RAFAEL_ID)

    expect(await screen.findByRole("status")).toHaveAttribute("aria-busy", "true")
  })

  it("celular: o cabeçalho com Voltar existe mesmo com erro", async () => {
    setDesktop(false)
    vi.mocked(service.getUser).mockRejectedValue({
      status: 404,
      code: "user_not_found",
      message: "x",
    })
    renderAt(RAFAEL_ID)

    await screen.findByRole("alert")

    expect(screen.getByRole("link", { name: "Voltar" })).toHaveAttribute(
      "href",
      "/usuarios",
    )
  })
})
