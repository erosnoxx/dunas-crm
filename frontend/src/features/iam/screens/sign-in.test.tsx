import { QueryClientProvider } from "@tanstack/react-query"
import { render, screen, waitFor } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import { createRoutesStub, Outlet } from "react-router"
import { beforeEach, describe, expect, it, vi } from "vitest"

import { createQueryClient } from "@/shared/lib/query-client"

import * as service from "../iam.service"
import { SignIn } from "./sign-in"

vi.mock("../iam.service")

const INVALID_CREDENTIALS =
  "E-mail ou senha incorretos. Confira os dados e tente de novo."
const wrongCredentials = (detail: string) => ({
  status: 401,
  code: "invalid_credentials",
  message: detail,
})
const rateLimited = {
  status: 429,
  code: "rate_limited",
  message: "muitas tentativas; tente de novo em alguns minutos",
}
const offline = { status: 0, message: "não foi possível conectar ao servidor" }
const session = { expiresAt: "2026-12-19T10:00:00Z", mustChangePassword: false }

function renderSignIn() {
  const client = createQueryClient(() => undefined)
  const Stub = createRoutesStub([
    {
      Component: () => (
        <QueryClientProvider client={client}>
          <Outlet />
        </QueryClientProvider>
      ),
      children: [
        { path: "/entrar", Component: SignIn },
        { path: "/", Component: () => <p>início</p> },
        { path: "/conta/trocar-senha", Component: () => <p>trocar senha</p> },
        { path: "/esqueci-senha", Component: () => <p>esqueci a senha</p> },
      ],
    },
  ])

  render(<Stub initialEntries={["/entrar"]} />)

  return userEvent.setup()
}

async function fillAndSubmit(
  user: ReturnType<typeof userEvent.setup>,
  email = "rafael@exemplo.com.br",
  password = "senhaerrada",
) {
  await user.type(screen.getByLabelText("E-mail"), email)
  await user.type(screen.getByLabelText("Senha"), password)
  await user.click(screen.getByRole("button", { name: "Entrar" }))
}

beforeEach(() => vi.resetAllMocks())

describe("SignIn", () => {
  it("mostra o título, o texto de abertura, os campos, o botão e as notas do mockup", () => {
    renderSignIn()

    expect(screen.getByRole("heading", { level: 1, name: "Entrar" })).toBeInTheDocument()
    expect(
      screen.getByText("Acesse o sistema da DUNAS com o e-mail do seu convite."),
    ).toBeInTheDocument()
    expect(screen.getByLabelText("E-mail")).toHaveAttribute("type", "email")
    expect(screen.getByLabelText("E-mail")).toHaveAttribute(
      "placeholder",
      "nome@empresa.com.br",
    )
    expect(screen.getByLabelText("Senha")).toHaveAttribute("type", "password")
    expect(screen.getByRole("button", { name: "Entrar" })).toBeEnabled()
    expect(
      screen.getByText(
        "O acesso é por convite. Se você ainda não tem conta, peça ao administrador.",
      ),
    ).toBeInTheDocument()
    expect(screen.queryByRole("alert")).not.toBeInTheDocument()
  })

  it("o campo e-mail corta o que passar de 254 caracteres ao colar", async () => {
    const user = renderSignIn()
    const email = screen.getByLabelText("E-mail")

    expect(email).toHaveAttribute("maxlength", "254")
    await user.click(email)
    await user.paste("a".repeat(255))

    expect(email).toHaveValue("a".repeat(254))
  })

  it("envia só e-mail e senha, sem aparar a senha, e vai para o início", async () => {
    vi.mocked(service.login).mockResolvedValue(session)
    const user = renderSignIn()

    await fillAndSubmit(user, "  rafael@exemplo.com.br ", " Senha#2026 ")

    expect(await screen.findByText("início")).toBeInTheDocument()
    expect(service.login).toHaveBeenCalledTimes(1)
    // O corpo é só e-mail e senha: sem `device`.
    expect(vi.mocked(service.login).mock.calls[0][0]).toEqual({
      email: "rafael@exemplo.com.br",
      password: " Senha#2026 ",
    })
  })

  it("com troca de senha obrigatória vai para a troca", async () => {
    vi.mocked(service.login).mockResolvedValue({ ...session, mustChangePassword: true })
    const user = renderSignIn()

    await fillAndSubmit(user)

    expect(await screen.findByText("trocar senha")).toBeInTheDocument()
  })

  it("e-mail ou senha vazios mostram o erro do campo e não chamam a API", async () => {
    const user = renderSignIn()

    await user.click(screen.getByRole("button", { name: "Entrar" }))

    expect(await screen.findByText("informe o e-mail")).toBeInTheDocument()
    expect(screen.getByText("informe a senha")).toBeInTheDocument()
    expect(service.login).not.toHaveBeenCalled()
  })

  it("invalid_credentials mostra o aviso de erro e mantém os campos preenchidos", async () => {
    vi.mocked(service.login).mockRejectedValue(
      wrongCredentials("e-mail ou senha incorretos"),
    )
    const user = renderSignIn()

    await fillAndSubmit(user)

    const alert = await screen.findByRole("alert")
    expect(alert).toHaveTextContent(INVALID_CREDENTIALS)
    expect(alert).toHaveClass("border-danger")
    expect(screen.getByLabelText("E-mail")).toHaveValue("rafael@exemplo.com.br")
    expect(screen.getByLabelText("Senha")).toHaveValue("senhaerrada")
    expect(screen.getByRole("button", { name: "Entrar" })).toBeEnabled()
  })

  it.each([
    ["e-mail que não existe", "ninguem@exemplo.com.br", "e-mail ou senha incorretos"],
    ["senha errada", "rafael@exemplo.com.br", "a senha não confere"],
    ["usuário desativado", "desativado@exemplo.com.br", "usuário desativado"],
  ])("a mesma mensagem para %s", async (_caso, email, detail) => {
    vi.mocked(service.login).mockRejectedValue(wrongCredentials(detail))
    const user = renderSignIn()

    await fillAndSubmit(user, email)

    const alert = await screen.findByRole("alert")
    expect(alert).toHaveTextContent(INVALID_CREDENTIALS)
    expect(alert.textContent).toBe(INVALID_CREDENTIALS)
  })

  it("rate_limited avisa, desliga e-mail, senha, mostrar e botão, e não reenvia sozinho", async () => {
    vi.mocked(service.login).mockRejectedValue(rateLimited)
    const user = renderSignIn()

    await fillAndSubmit(user)

    const alert = await screen.findByRole("alert")
    expect(alert).toHaveClass("bg-highlight")
    expect(alert).toHaveTextContent("Muitas tentativas de entrada")
    expect(alert).toHaveTextContent(
      "Por segurança, o acesso fica pausado por 15 minutos. Tente de novo depois desse prazo.",
    )
    expect(screen.getByLabelText("E-mail")).toBeDisabled()
    expect(screen.getByLabelText("Senha")).toBeDisabled()
    expect(screen.getByRole("button", { name: "Mostrar" })).toBeDisabled()
    expect(screen.getByRole("button", { name: "Entrar" })).toBeDisabled()
    // O jsdom não calcula a cascata: confere o conjunto efetivo de classes. O
    // `disabled:opacity-50` do Button tem mais força que `opacity-100` e deixaria o botão pela metade.
    const locked = screen.getByRole("button", { name: "Entrar" })
    expect(locked).toHaveClass("bg-line", "text-text-muted", "disabled:opacity-100")
    expect(locked).not.toHaveClass("disabled:opacity-50", "bg-brand", "text-on-brand")
    expect(screen.getByRole("link", { name: "Esqueci minha senha" })).toBeInTheDocument()

    await user.click(screen.getByRole("button", { name: "Entrar" }))
    await new Promise((resolve) => setTimeout(resolve, 50))
    expect(service.login).toHaveBeenCalledTimes(1)
  })

  it("erro de rede mostra a mensagem de conexão e deixa tentar de novo", async () => {
    vi.mocked(service.login).mockRejectedValueOnce(offline).mockResolvedValue(session)
    const user = renderSignIn()

    await fillAndSubmit(user)

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Não foi possível conectar ao servidor",
    )
    expect(screen.getByLabelText("E-mail")).toBeEnabled()
    expect(screen.getByRole("button", { name: "Entrar" })).toBeEnabled()

    await user.click(screen.getByRole("button", { name: "Entrar" }))

    expect(await screen.findByText("início")).toBeInTheDocument()
    expect(service.login).toHaveBeenCalledTimes(2)
  })

  it("erro sem mensagem mostra o texto genérico da API em vez de um aviso vazio", async () => {
    vi.mocked(service.login).mockRejectedValue(new Error(""))
    const user = renderSignIn()

    await fillAndSubmit(user)

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Algo deu errado; tente de novo",
    )
  })

  it("outro erro da API mostra a mensagem que ela mandou", async () => {
    vi.mocked(service.login).mockRejectedValue({
      status: 500,
      message: "algo deu errado; tente de novo",
    })
    const user = renderSignIn()

    await fillAndSubmit(user)

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Algo deu errado; tente de novo",
    )
  })

  it("durante o envio o botão carrega e um clique só dispara um pedido", async () => {
    let finish: (value: typeof session) => void = () => undefined
    vi.mocked(service.login).mockReturnValue(
      new Promise((resolve) => {
        finish = resolve
      }),
    )
    const user = renderSignIn()

    await fillAndSubmit(user)
    const button = await screen.findByRole("button", { name: "Entrar" })
    await waitFor(() => expect(button).toHaveAttribute("aria-busy", "true"))
    await user.click(button)
    await user.type(screen.getByLabelText("Senha"), "{Enter}")

    expect(service.login).toHaveBeenCalledTimes(1)

    finish(session)
    expect(await screen.findByText("início")).toBeInTheDocument()
  })

  it("Esqueci minha senha vai para /esqueci-senha", async () => {
    const user = renderSignIn()

    await user.click(screen.getByRole("link", { name: "Esqueci minha senha" }))

    expect(await screen.findByText("esqueci a senha")).toBeInTheDocument()
  })

  it("o botão e o link têm alvo de toque de 44 px ou mais", () => {
    renderSignIn()

    expect(screen.getByRole("button", { name: "Entrar" })).toHaveClass("min-h-13")
    expect(screen.getByRole("link", { name: "Esqueci minha senha" })).toHaveClass(
      "min-h-11",
    )
  })
})

describe("teclado", () => {
  it("percorre os controles pelo Tab na ordem da tela", async () => {
    const user = renderSignIn()

    await user.tab()
    expect(screen.getByLabelText("E-mail")).toHaveFocus()
    await user.tab()
    expect(screen.getByLabelText("Senha")).toHaveFocus()
    await user.tab()
    expect(screen.getByRole("button", { name: "Mostrar" })).toHaveFocus()
    await user.tab()
    expect(screen.getByRole("button", { name: "Entrar" })).toHaveFocus()
    await user.tab()
    expect(screen.getByRole("link", { name: "Esqueci minha senha" })).toHaveFocus()
  })
})
