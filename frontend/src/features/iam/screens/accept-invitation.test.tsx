import { QueryClientProvider } from "@tanstack/react-query"
import { render, screen, waitFor } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import { createRoutesStub, Outlet } from "react-router"
import { beforeEach, describe, expect, it, vi } from "vitest"

import { createQueryClient } from "@/shared/lib/query-client"

import * as service from "../iam.service"
import { AcceptInvitationScreen } from "./accept-invitation"

vi.mock("../iam.service")

const PASSWORD = "Dunasolar#2026"
const session = { expiresAt: "2026-12-19T10:00:00Z", mustChangePassword: false }
const apiError = (status: number, code: string, message: string) => ({
  status,
  code,
  message,
})

function renderScreen(url = "/convite?token=segredo-do-link") {
  const client = createQueryClient(() => undefined)
  const invalidate = vi.spyOn(client, "invalidateQueries")
  const Stub = createRoutesStub([
    {
      Component: () => (
        <QueryClientProvider client={client}>
          <Outlet />
        </QueryClientProvider>
      ),
      children: [
        { path: "/convite", Component: AcceptInvitationScreen },
        { path: "/", Component: () => <p>início</p> },
        { path: "/entrar", Component: () => <p>tela de entrar</p> },
      ],
    },
  ])

  render(<Stub initialEntries={[url]} />)

  return { user: userEvent.setup(), invalidate }
}

const passwordInput = () => screen.getByLabelText("Nova senha")
const repeatInput = () => screen.getByLabelText("Repita a senha")
const submitButton = () => screen.getByRole("button", { name: "Criar senha e entrar" })

async function fill(
  user: ReturnType<typeof userEvent.setup>,
  password = PASSWORD,
  repeat = password,
) {
  await user.type(passwordInput(), password)
  await user.type(repeatInput(), repeat)
}

beforeEach(() => vi.resetAllMocks())

describe("AcceptInvitationScreen", () => {
  it("mostra o título, o texto de apoio e os campos, sem e-mail nem papel, e não chama a API", async () => {
    renderScreen()

    expect(
      await screen.findByRole("heading", { level: 1, name: "Crie sua senha" }),
    ).toBeInTheDocument()
    expect(screen.getByText("Defina uma senha para entrar.")).toBeInTheDocument()
    expect(passwordInput()).toHaveAttribute("type", "password")
    expect(repeatInput()).toHaveAttribute("type", "password")
    expect(screen.getByText("0 de 30")).toBeInTheDocument()
    expect(screen.queryByText(/@/)).not.toBeInTheDocument()
    expect(screen.queryByText(/administrador|comercial|técnico/i)).not.toBeInTheDocument()
    expect(submitButton()).toBeDisabled()
    expect(screen.queryByRole("alert")).not.toBeInTheDocument()
    for (const fn of Object.values(service)) expect(fn).not.toHaveBeenCalled()
  })

  it("o checklist e o contador acompanham a digitação", async () => {
    const { user } = renderScreen()
    await screen.findByRole("heading", { name: "Crie sua senha" })

    await user.type(passwordInput(), "Dunasolar")

    expect(screen.getByText("9 de 30")).toBeInTheDocument()
    const items = screen.getAllByRole("listitem")
    expect(items).toHaveLength(5)
    expect(items[0]).toHaveTextContent("De 8 a 30 caracteres, atendida")
    expect(items[1]).toHaveTextContent("Uma letra maiúscula, atendida")
    expect(items[2]).toHaveTextContent("Uma letra minúscula, atendida")
    expect(items[3]).toHaveTextContent("Um número, ainda não atendida")
    expect(items[4]).toHaveTextContent(
      "Um caractere especial, como ! ou #, ainda não atendida",
    )
  })

  it("o botão só liga com as 5 regras e a repetição iguais", async () => {
    const { user } = renderScreen()
    await screen.findByRole("heading", { name: "Crie sua senha" })

    await user.type(passwordInput(), PASSWORD)
    expect(submitButton()).toBeDisabled()

    await user.type(repeatInput(), "Dunasolar#2025")
    expect(submitButton()).toBeDisabled()

    await user.clear(repeatInput())
    await user.type(repeatInput(), PASSWORD)
    await waitFor(() => expect(submitButton()).toBeEnabled())

    await user.type(passwordInput(), "x")
    await waitFor(() => expect(submitButton()).toBeDisabled())
  })

  it("com regra faltando o botão fica desligado mesmo com a repetição igual", async () => {
    const { user } = renderScreen()
    await screen.findByRole("heading", { name: "Crie sua senha" })

    await fill(user, "Dunasolar2026")

    expect(submitButton()).toBeDisabled()
    expect(service.acceptInvitation).not.toHaveBeenCalled()
  })

  it("a repetição diferente mostra o erro mesmo quando a senha tem outras falhas", async () => {
    const { user } = renderScreen()
    await screen.findByRole("heading", { name: "Crie sua senha" })

    await fill(user, "abc", "xyz")

    expect(await screen.findByText("as senhas não são iguais")).toBeInTheDocument()
    expect(submitButton()).toBeDisabled()
  })

  it("mudar a senha depois revalida a repetição", async () => {
    const { user } = renderScreen()
    await screen.findByRole("heading", { name: "Crie sua senha" })

    await fill(user, PASSWORD)
    await user.type(passwordInput(), "!")

    expect(await screen.findByText("as senhas não são iguais")).toBeInTheDocument()
  })

  it("envia token e senha sem aparar, abre a sessão e vai para o início", async () => {
    vi.mocked(service.acceptInvitation).mockResolvedValue(session)
    const { user, invalidate } = renderScreen()
    await screen.findByRole("heading", { name: "Crie sua senha" })

    await fill(user, " Dunasolar#2026 ")
    await waitFor(() => expect(submitButton()).toBeEnabled())
    await user.click(submitButton())

    expect(await screen.findByText("início")).toBeInTheDocument()
    expect(service.acceptInvitation).toHaveBeenCalledTimes(1)
    // O 1º argumento é o corpo; o react-query ainda passa o contexto da mutação.
    expect(vi.mocked(service.acceptInvitation).mock.calls[0][0]).toEqual({
      token: "segredo-do-link",
      password: " Dunasolar#2026 ",
    })
    expect(invalidate).toHaveBeenCalledWith({ queryKey: ["me"] })
  })

  it("durante o envio o botão carrega e um clique só dispara um pedido", async () => {
    let finish: (value: typeof session) => void = () => undefined
    vi.mocked(service.acceptInvitation).mockReturnValue(
      new Promise((resolve) => {
        finish = resolve
      }),
    )
    const { user } = renderScreen()
    await screen.findByRole("heading", { name: "Crie sua senha" })
    await fill(user)
    await waitFor(() => expect(submitButton()).toBeEnabled())

    await user.click(submitButton())
    await waitFor(() => expect(submitButton()).toHaveAttribute("aria-busy", "true"))
    await user.click(submitButton())
    await user.type(repeatInput(), "{Enter}")

    expect(service.acceptInvitation).toHaveBeenCalledTimes(1)

    finish(session)
    expect(await screen.findByText("início")).toBeInTheDocument()
  })

  it.each([
    ["link_expired", "Este convite expirou"],
    ["link_used", "Este convite já foi usado ou foi substituído"],
  ])("%s troca a tela pelo aviso de link", async (code, title) => {
    vi.mocked(service.acceptInvitation).mockRejectedValue(
      apiError(422, code, "mensagem do servidor"),
    )
    const { user } = renderScreen()
    await screen.findByRole("heading", { name: "Crie sua senha" })
    await fill(user)
    await waitFor(() => expect(submitButton()).toBeEnabled())

    await user.click(submitButton())

    expect(
      await screen.findByRole("heading", { level: 1, name: title }),
    ).toBeInTheDocument()
    expect(
      screen.queryByRole("heading", { name: "Crie sua senha" }),
    ).not.toBeInTheDocument()
    expect(screen.getByRole("link", { name: "Ir para o login" })).toBeInTheDocument()
  })

  it("invalid_password mostra a mensagem do primeiro critério que falta, e mantém os campos", async () => {
    vi.mocked(service.acceptInvitation).mockRejectedValue(
      apiError(400, "invalid_password", "a senha precisa ter um caractere especial"),
    )
    const { user } = renderScreen()
    await screen.findByRole("heading", { name: "Crie sua senha" })
    await fill(user)
    await waitFor(() => expect(submitButton()).toBeEnabled())

    await user.click(submitButton())

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "A senha precisa ter um caractere especial",
    )
    expect(passwordInput()).toHaveValue(PASSWORD)
    expect(repeatInput()).toHaveValue(PASSWORD)
  })

  it("erro de rede mostra o aviso e deixa tentar de novo", async () => {
    vi.mocked(service.acceptInvitation)
      .mockRejectedValueOnce({
        status: 0,
        message: "não foi possível conectar ao servidor",
      })
      .mockResolvedValue(session)
    const { user } = renderScreen()
    await screen.findByRole("heading", { name: "Crie sua senha" })
    await fill(user)
    await waitFor(() => expect(submitButton()).toBeEnabled())

    await user.click(submitButton())

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Não foi possível conectar ao servidor",
    )
    expect(submitButton()).toBeEnabled()

    await user.click(submitButton())

    expect(await screen.findByText("início")).toBeInTheDocument()
    expect(service.acceptInvitation).toHaveBeenCalledTimes(2)
  })

  it.each([
    ["sem token na URL", "/convite"],
    ["com token vazio", "/convite?token="],
  ])("%s mostra o link usado e não chama a API", async (_caso, url) => {
    renderScreen(url)

    expect(
      await screen.findByRole("heading", {
        level: 1,
        name: "Este convite já foi usado ou foi substituído",
      }),
    ).toBeInTheDocument()
    expect(screen.queryByLabelText("Nova senha")).not.toBeInTheDocument()
    for (const fn of Object.values(service)) expect(fn).not.toHaveBeenCalled()
  })

  it("o botão e os campos têm alvo de toque de 44 px ou mais", async () => {
    renderScreen()
    await screen.findByRole("heading", { name: "Crie sua senha" })

    expect(submitButton()).toHaveClass("min-h-13")
    expect(passwordInput()).toHaveClass("h-13")
    expect(repeatInput()).toHaveClass("h-13")
  })
})

describe("teclado", () => {
  it("percorre os controles pelo Tab na ordem da tela", async () => {
    const { user } = renderScreen()

    await user.tab()
    expect(passwordInput()).toHaveFocus()
    await user.tab()
    expect(screen.getByRole("button", { name: "Mostrar" })).toHaveFocus()
    await user.tab()
    expect(screen.getByLabelText("Repita a senha")).toHaveFocus()
    // O botão só entra na ordem quando as regras e a repetição estão atendidas.
    await user.type(passwordInput(), "Dunas#2026")
    await user.type(screen.getByLabelText("Repita a senha"), "Dunas#2026")
    await user.tab()
    expect(screen.getByRole("button", { name: "Criar senha e entrar" })).toHaveFocus()
  })
})
