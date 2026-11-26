import { QueryClientProvider } from "@tanstack/react-query"
import { render, screen, waitFor } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import { createRoutesStub, Outlet } from "react-router"
import { beforeEach, describe, expect, it, vi } from "vitest"

import { createQueryClient } from "@/shared/lib/query-client"

import * as service from "../iam.service"
import { ForgotPassword } from "./forgot-password"

vi.mock("../iam.service")

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
        { path: "/esqueci-senha", Component: ForgotPassword },
        { path: "/entrar", Component: () => <p>tela de entrar</p> },
      ],
    },
  ])

  render(<Stub initialEntries={["/esqueci-senha"]} />)

  return userEvent.setup()
}

const emailInput = () => screen.getByLabelText("E-mail")
const submit = () => screen.getByRole("button", { name: "Enviar link" })
const SENT =
  "Se o e-mail estiver cadastrado, enviamos um link para criar uma nova senha. O link vale por 1 hora."

beforeEach(() => vi.resetAllMocks())

describe("ForgotPassword", () => {
  it("mostra título, texto de apoio, campo, botão e a volta para o login, sem chamar a API", async () => {
    renderScreen()

    expect(
      await screen.findByRole("heading", { level: 1, name: "Esqueceu a senha?" }),
    ).toBeInTheDocument()
    expect(
      screen.getByText(
        "Informe o e-mail da sua conta. Se ele existir, enviamos um link para criar uma nova senha.",
      ),
    ).toBeInTheDocument()
    expect(emailInput()).toHaveAttribute("type", "email")
    expect(submit()).toBeEnabled()
    expect(screen.getByRole("link", { name: "Voltar para o login" })).toHaveAttribute(
      "href",
      "/entrar",
    )
    for (const fn of Object.values(service)) expect(fn).not.toHaveBeenCalled()
  })

  it("o campo e-mail corta o que passar de 254 caracteres ao colar", async () => {
    const user = renderScreen()
    const email = screen.getByLabelText("E-mail")

    expect(email).toHaveAttribute("maxlength", "254")
    await user.click(email)
    await user.paste("a".repeat(255))

    expect(email).toHaveValue("a".repeat(254))
  })

  it("e-mail vazio ou inválido mostra o erro do campo e não chama a API", async () => {
    const user = renderScreen()
    await screen.findByRole("heading", { name: "Esqueceu a senha?" })

    await user.click(submit())
    expect(await screen.findByText("informe o e-mail")).toBeInTheDocument()

    await user.type(emailInput(), "sem-arroba")
    await user.click(submit())
    expect(await screen.findByText("informe um e-mail válido")).toBeInTheDocument()
    expect(service.requestPasswordReset).not.toHaveBeenCalled()
  })

  it("com qualquer e-mail mostra o aviso aprovado, o mesmo texto, sem dizer se existe", async () => {
    vi.mocked(service.requestPasswordReset).mockResolvedValue(undefined)
    const user = renderScreen()
    await screen.findByRole("heading", { name: "Esqueceu a senha?" })

    await user.type(emailInput(), "  Alguem@Exemplo.com.br ")
    await user.click(submit())

    expect(
      await screen.findByRole("heading", { level: 1, name: "Confira seu e-mail" }),
    ).toBeInTheDocument()
    expect(screen.getByText(SENT)).toBeInTheDocument()
    expect(screen.queryByText(/alguem@/i)).not.toBeInTheDocument()
    expect(screen.queryByRole("alert")).not.toBeInTheDocument()
    expect(vi.mocked(service.requestPasswordReset).mock.calls[0][0]).toEqual({
      email: "Alguem@Exemplo.com.br",
    })
    expect(screen.getByRole("link", { name: "Voltar para o login" })).toHaveAttribute(
      "href",
      "/entrar",
    )
  })

  it("Pedir o link de novo volta ao formulário com o e-mail digitado", async () => {
    vi.mocked(service.requestPasswordReset).mockResolvedValue(undefined)
    const user = renderScreen()
    await screen.findByRole("heading", { name: "Esqueceu a senha?" })
    await user.type(emailInput(), "a@b.com")
    await user.click(submit())
    await screen.findByRole("heading", { name: "Confira seu e-mail" })

    await user.click(screen.getByRole("button", { name: "Pedir o link de novo" }))

    expect(
      await screen.findByRole("heading", { name: "Esqueceu a senha?" }),
    ).toBeInTheDocument()
    expect(emailInput()).toHaveValue("a@b.com")
  })

  it("rate_limited mostra o aviso de espera e deixa tentar depois", async () => {
    vi.mocked(service.requestPasswordReset)
      .mockRejectedValueOnce({
        status: 429,
        code: "rate_limited",
        message: "muitas tentativas",
      })
      .mockResolvedValue(undefined)
    const user = renderScreen()
    await screen.findByRole("heading", { name: "Esqueceu a senha?" })
    await user.type(emailInput(), "a@b.com")

    await user.click(submit())

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Muitas tentativas; tente de novo em alguns minutos",
    )
    expect(screen.queryByText(SENT)).not.toBeInTheDocument()

    await user.click(submit())

    expect(await screen.findByText(SENT)).toBeInTheDocument()
  })

  it("erro de rede mostra o aviso e não mostra o link enviado", async () => {
    vi.mocked(service.requestPasswordReset).mockRejectedValue({
      status: 0,
      message: "não foi possível conectar ao servidor",
    })
    const user = renderScreen()
    await screen.findByRole("heading", { name: "Esqueceu a senha?" })
    await user.type(emailInput(), "a@b.com")

    await user.click(submit())

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Não foi possível conectar ao servidor",
    )
    expect(screen.queryByText(SENT)).not.toBeInTheDocument()
  })

  it("durante o envio o botão carrega e um clique só dispara um pedido", async () => {
    let finish: () => void = () => undefined
    vi.mocked(service.requestPasswordReset).mockReturnValue(
      new Promise<void>((resolve) => {
        finish = resolve
      }),
    )
    const user = renderScreen()
    await screen.findByRole("heading", { name: "Esqueceu a senha?" })
    await user.type(emailInput(), "a@b.com")

    await user.click(submit())
    await waitFor(() => expect(submit()).toHaveAttribute("aria-busy", "true"))
    await user.click(submit())

    expect(service.requestPasswordReset).toHaveBeenCalledTimes(1)

    finish()
    expect(await screen.findByText(SENT)).toBeInTheDocument()
  })
})

describe("teclado", () => {
  it("percorre os controles pelo Tab na ordem da tela", async () => {
    const user = renderScreen()

    await user.tab()
    expect(screen.getByLabelText("E-mail")).toHaveFocus()
    await user.tab()
    expect(screen.getByRole("button", { name: "Enviar link" })).toHaveFocus()
    await user.tab()
    expect(screen.getByRole("link", { name: "Voltar para o login" })).toHaveFocus()
  })
})
