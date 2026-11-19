import { QueryClientProvider } from "@tanstack/react-query"
import { render, screen, waitFor } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import { createRoutesStub, Outlet } from "react-router"
import { toast } from "sonner"
import { beforeEach, describe, expect, it, vi } from "vitest"

import { createQueryClient } from "@/shared/lib/query-client"

import * as service from "../iam.service"
import type { CurrentUser } from "../iam.service"
import { ChangePasswordScreen } from "./change-password"

vi.mock("../iam.service")
vi.mock("sonner", () => ({ toast: { success: vi.fn() } }))

const CURRENT = "Provisoria#1"
const NEXT = "Dunasolar#2026"
const apiError = (status: number, code: string, message: string) => ({
  status,
  code,
  message,
})
const me = (mustChangePassword: boolean): CurrentUser => ({
  id: "1",
  name: "Ana",
  email: "ana@dunas.com",
  role: "ADMINISTRATOR",
  mustChangePassword,
})

function renderScreen(mustChangePassword: boolean) {
  vi.mocked(service.getCurrentUser).mockResolvedValue(me(mustChangePassword))
  const client = createQueryClient(() => undefined)
  const Stub = createRoutesStub([
    {
      Component: () => (
        <QueryClientProvider client={client}>
          <Outlet />
        </QueryClientProvider>
      ),
      children: [
        { path: "/conta/trocar-senha", Component: ChangePasswordScreen },
        { path: "/", Component: () => <p>início</p> },
        { path: "/entrar", Component: () => <p>tela de entrar</p> },
      ],
    },
  ])

  render(<Stub initialEntries={["/conta/trocar-senha"]} />)

  return { user: userEvent.setup(), client }
}

const currentInput = () => screen.getByLabelText("Senha atual")
const newInput = () => screen.getByLabelText("Nova senha")
const repeatInput = () => screen.getByLabelText("Repita a senha")

async function fill(
  user: ReturnType<typeof userEvent.setup>,
  current = CURRENT,
  next = NEXT,
  repeat = next,
) {
  await user.type(currentInput(), current)
  await user.type(newInput(), next)
  await user.type(repeatInput(), repeat)
}

beforeEach(() => vi.resetAllMocks())

describe("ChangePasswordScreen, voluntária", () => {
  const submit = () => screen.getByRole("button", { name: "Salvar nova senha" })

  it("mostra título, campos, checklist e o voltar, sem Sair", async () => {
    renderScreen(false)

    expect(
      await screen.findByRole("heading", { level: 1, name: "Trocar senha" }),
    ).toBeInTheDocument()
    expect(currentInput()).toHaveAttribute("type", "password")
    expect(newInput()).toHaveAttribute("type", "password")
    expect(repeatInput()).toHaveAttribute("type", "password")
    expect(screen.getByText("0 de 30")).toBeInTheDocument()
    expect(screen.getByRole("list", { name: "Regras da senha" })).toBeInTheDocument()
    expect(screen.getByRole("link", { name: "Voltar" })).toHaveAttribute("href", "/")
    expect(screen.queryByRole("button", { name: "Sair" })).not.toBeInTheDocument()
    expect(submit()).toBeDisabled()
  })

  it("só liga o botão com a senha atual, as 5 regras e a repetição iguais", async () => {
    const { user } = renderScreen(false)
    await screen.findByRole("heading", { name: "Trocar senha" })

    await fill(user, CURRENT, NEXT, "outra")
    expect(submit()).toBeDisabled()

    await user.clear(repeatInput())
    await user.type(repeatInput(), NEXT)
    expect(submit()).toBeEnabled()
  })

  it("envia as senhas como digitadas, avisa e volta para o início", async () => {
    vi.mocked(service.changePassword).mockResolvedValue(undefined)
    const { user } = renderScreen(false)
    await screen.findByRole("heading", { name: "Trocar senha" })

    await fill(user, "Provisoria#1 ", "Dunasolar#2026 ")
    await user.click(submit())

    await waitFor(() => expect(service.changePassword).toHaveBeenCalledTimes(1))
    expect(vi.mocked(service.changePassword).mock.calls[0][0]).toEqual({
      currentPassword: "Provisoria#1 ",
      newPassword: "Dunasolar#2026 ",
    })
    expect(toast.success).toHaveBeenCalledWith("Senha alterada")
    expect(await screen.findByText("início")).toBeInTheDocument()
  })

  it("não bloqueia uma nova senha igual à atual", async () => {
    vi.mocked(service.changePassword).mockResolvedValue(undefined)
    const { user } = renderScreen(false)
    await screen.findByRole("heading", { name: "Trocar senha" })

    await fill(user, NEXT, NEXT)
    expect(submit()).toBeEnabled()
    await user.click(submit())

    await waitFor(() => expect(service.changePassword).toHaveBeenCalledTimes(1))
    expect(vi.mocked(service.changePassword).mock.calls[0][0]).toEqual({
      currentPassword: NEXT,
      newPassword: NEXT,
    })
  })

  it("wrong_current_password aparece no campo da senha atual, sem alerta", async () => {
    vi.mocked(service.changePassword).mockRejectedValue(
      apiError(422, "wrong_current_password", "texto da api"),
    )
    const { user } = renderScreen(false)
    await screen.findByRole("heading", { name: "Trocar senha" })

    await fill(user)
    await user.click(submit())

    expect(await screen.findByText("a senha atual está incorreta")).toBeInTheDocument()
    expect(currentInput()).toHaveAttribute("aria-invalid", "true")
    expect(currentInput()).toHaveAccessibleDescription("a senha atual está incorreta")
    expect(screen.queryByRole("alert")).not.toBeInTheDocument()
    expect(screen.queryByText("início")).not.toBeInTheDocument()
  })

  it("invalid_password mostra o detail da API num alerta", async () => {
    vi.mocked(service.changePassword).mockRejectedValue(
      apiError(400, "invalid_password", "a senha precisa de um número"),
    )
    const { user } = renderScreen(false)
    await screen.findByRole("heading", { name: "Trocar senha" })

    await fill(user)
    await user.click(submit())

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "A senha precisa de um número",
    )
    expect(screen.queryByText("a senha atual está incorreta")).not.toBeInTheDocument()
  })
})

describe("ChangePasswordScreen, obrigatória", () => {
  const submit = () => screen.getByRole("button", { name: "Salvar e continuar" })

  it("mostra título e texto aprovados, sem voltar nem outro destino além de Sair", async () => {
    renderScreen(true)

    expect(
      await screen.findByRole("heading", { level: 1, name: "Troque a senha inicial" }),
    ).toBeInTheDocument()
    expect(
      screen.getByText(
        "Sua conta foi criada com uma senha provisória. Defina a sua para continuar.",
      ),
    ).toBeInTheDocument()
    expect(currentInput()).toBeInTheDocument()
    expect(screen.queryByRole("link")).not.toBeInTheDocument()
    expect(screen.queryByRole("navigation")).not.toBeInTheDocument()
    expect(screen.getByRole("button", { name: "Sair" })).toBeInTheDocument()
    expect(submit()).toBeDisabled()
  })

  it("Sair encerra a sessão e vai para o login", async () => {
    vi.mocked(service.logout).mockResolvedValue(undefined)
    const { user } = renderScreen(true)
    await screen.findByRole("heading", { name: "Troque a senha inicial" })

    await user.click(screen.getByRole("button", { name: "Sair" }))

    expect(service.logout).toHaveBeenCalledTimes(1)
    expect(await screen.findByText("tela de entrar")).toBeInTheDocument()
  })

  it("ao trocar, atualiza o me (mustChangePassword vira false) e vai para o início", async () => {
    vi.mocked(service.changePassword).mockResolvedValue(undefined)
    const { user, client } = renderScreen(true)
    await screen.findByRole("heading", { name: "Troque a senha inicial" })
    vi.mocked(service.getCurrentUser).mockResolvedValue(me(false))

    await fill(user)
    await user.click(submit())

    expect(await screen.findByText("início")).toBeInTheDocument()
    expect(client.getQueryData<CurrentUser>(["me"])?.mustChangePassword).toBe(false)
  })
})
