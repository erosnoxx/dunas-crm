import { QueryClientProvider } from "@tanstack/react-query"
import { render, screen, waitFor } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import { createRoutesStub, Outlet } from "react-router"
import { toast } from "sonner"
import { beforeEach, describe, expect, it, vi } from "vitest"

import { createQueryClient } from "@/shared/lib/query-client"

import * as service from "../iam.service"
import { ResetPasswordScreen } from "./reset-password"

vi.mock("../iam.service")
vi.mock("sonner", () => ({ toast: { success: vi.fn() } }))

const PASSWORD = "Dunasolar#2026"
const HEADING = "Crie uma nova senha"
const apiError = (status: number, code: string, message: string) => ({
  status,
  code,
  message,
})

function renderScreen(url = "/redefinir-senha?token=segredo-do-link") {
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
        { path: "/redefinir-senha", Component: ResetPasswordScreen },
        { path: "/entrar", Component: () => <p>tela de entrar</p> },
        { path: "/esqueci-senha", Component: () => <p>tela de esqueci a senha</p> },
      ],
    },
  ])

  render(<Stub initialEntries={[url]} />)

  return { user: userEvent.setup(), invalidate }
}

const passwordInput = () => screen.getByLabelText("Nova senha")
const repeatInput = () => screen.getByLabelText("Repita a senha")
const submitButton = () => screen.getByRole("button", { name: "Salvar nova senha" })

async function fillAndSubmit(
  user: ReturnType<typeof userEvent.setup>,
  password = PASSWORD,
) {
  await screen.findByRole("heading", { name: HEADING })
  await user.type(passwordInput(), password)
  await user.type(repeatInput(), password)
  await waitFor(() => expect(submitButton()).toBeEnabled())
  await user.click(submitButton())
}

beforeEach(() => vi.resetAllMocks())

describe("ResetPasswordScreen", () => {
  it("mostra título, texto de apoio, campos com contador e checklist, botão desligado, sem chamar a API", async () => {
    renderScreen()

    expect(
      await screen.findByRole("heading", { level: 1, name: HEADING }),
    ).toBeInTheDocument()
    expect(screen.getByText("Defina uma senha para voltar a entrar.")).toBeInTheDocument()
    expect(passwordInput()).toHaveAttribute("type", "password")
    expect(repeatInput()).toHaveAttribute("type", "password")
    expect(screen.getByText("0 de 30")).toBeInTheDocument()
    expect(screen.getAllByRole("listitem")).toHaveLength(5)
    expect(submitButton()).toBeDisabled()
    expect(screen.queryByRole("alert")).not.toBeInTheDocument()
    for (const fn of Object.values(service)) expect(fn).not.toHaveBeenCalled()
  })

  it("o botão só liga com as 5 regras e a repetição iguais", async () => {
    const { user } = renderScreen()
    await screen.findByRole("heading", { name: HEADING })

    await user.type(passwordInput(), PASSWORD)
    await user.type(repeatInput(), "Dunasolar#2025")
    expect(submitButton()).toBeDisabled()
    expect(await screen.findByText("as senhas não são iguais")).toBeInTheDocument()

    await user.clear(repeatInput())
    await user.type(repeatInput(), PASSWORD)
    await waitFor(() => expect(submitButton()).toBeEnabled())
  })

  it("envia token e senha sem aparar, vai para /entrar com o aviso e não abre sessão", async () => {
    vi.mocked(service.resetPassword).mockResolvedValue(undefined)
    const { user, invalidate } = renderScreen()

    await fillAndSubmit(user, " Dunasolar#2026 ")

    expect(await screen.findByText("tela de entrar")).toBeInTheDocument()
    expect(vi.mocked(service.resetPassword).mock.calls[0][0]).toEqual({
      token: "segredo-do-link",
      password: " Dunasolar#2026 ",
    })
    expect(toast.success).toHaveBeenCalledWith("Senha alterada")
    expect(invalidate).not.toHaveBeenCalled()
    expect(service.getCurrentUser).not.toHaveBeenCalled()
    expect(service.login).not.toHaveBeenCalled()
  })

  it("durante o envio o botão carrega e um clique só dispara um pedido", async () => {
    let finish: () => void = () => undefined
    vi.mocked(service.resetPassword).mockReturnValue(
      new Promise<void>((resolve) => {
        finish = resolve
      }),
    )
    const { user } = renderScreen()
    await fillAndSubmit(user)
    await waitFor(() => expect(submitButton()).toHaveAttribute("aria-busy", "true"))
    await user.click(submitButton())

    expect(service.resetPassword).toHaveBeenCalledTimes(1)

    finish()
    expect(await screen.findByText("tela de entrar")).toBeInTheDocument()
  })

  it.each([
    ["link_expired", "Este link expirou"],
    ["link_used", "Este link já foi usado ou foi substituído"],
  ])("%s troca a tela pelo aviso de link de redefinição", async (code, title) => {
    vi.mocked(service.resetPassword).mockRejectedValue(
      apiError(422, code, "mensagem do servidor"),
    )
    const { user } = renderScreen()

    await fillAndSubmit(user)

    expect(
      await screen.findByRole("heading", { level: 1, name: title }),
    ).toBeInTheDocument()
    expect(screen.queryByRole("heading", { name: HEADING })).not.toBeInTheDocument()
    expect(screen.getByRole("link", { name: "Pedir novo link" })).toHaveAttribute(
      "href",
      "/esqueci-senha",
    )
    expect(toast.success).not.toHaveBeenCalled()
  })

  it("invalid_password mostra a mensagem da API e mantém os campos", async () => {
    vi.mocked(service.resetPassword).mockRejectedValue(
      apiError(400, "invalid_password", "a senha precisa ter um caractere especial"),
    )
    const { user } = renderScreen()

    await fillAndSubmit(user)

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "A senha precisa ter um caractere especial",
    )
    expect(passwordInput()).toHaveValue(PASSWORD)
    expect(repeatInput()).toHaveValue(PASSWORD)
  })

  it("erro de rede mostra o aviso e deixa tentar de novo", async () => {
    vi.mocked(service.resetPassword)
      .mockRejectedValueOnce({
        status: 0,
        message: "não foi possível conectar ao servidor",
      })
      .mockResolvedValue(undefined)
    const { user } = renderScreen()

    await fillAndSubmit(user)

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Não foi possível conectar ao servidor",
    )
    await user.click(submitButton())

    expect(await screen.findByText("tela de entrar")).toBeInTheDocument()
    expect(service.resetPassword).toHaveBeenCalledTimes(2)
  })

  it.each([
    ["sem token na URL", "/redefinir-senha"],
    ["com token vazio", "/redefinir-senha?token="],
  ])("%s mostra o link usado e não chama a API", async (_caso, url) => {
    renderScreen(url)

    expect(
      await screen.findByRole("heading", {
        level: 1,
        name: "Este link já foi usado ou foi substituído",
      }),
    ).toBeInTheDocument()
    expect(screen.getByRole("link", { name: "Pedir novo link" })).toBeInTheDocument()
    for (const fn of Object.values(service)) expect(fn).not.toHaveBeenCalled()
  })
})
