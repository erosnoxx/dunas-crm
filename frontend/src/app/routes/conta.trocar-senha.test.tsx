import { QueryClientProvider } from "@tanstack/react-query"
import { render, screen } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import { createRoutesStub } from "react-router"
import { beforeEach, describe, expect, it, vi } from "vitest"

import * as service from "@/features/iam/iam.service"
import type { CurrentUser } from "@/features/iam/iam.service"
import { createQueryClient } from "@/shared/lib/query-client"

import Authenticated from "./authenticated"
import ContaTrocarSenha from "./conta.trocar-senha"

vi.mock("@/features/iam/iam.service")
vi.mock("sonner", () => ({ toast: { success: vi.fn() } }))

const me = (mustChangePassword: boolean): CurrentUser => ({
  id: "1",
  name: "Ana",
  email: "ana@dunas.com",
  role: "ADMINISTRATOR",
  mustChangePassword,
})

function renderAt(path: string) {
  const client = createQueryClient(() => undefined)
  const Stub = createRoutesStub([
    {
      Component: () => (
        <QueryClientProvider client={client}>
          <Authenticated />
        </QueryClientProvider>
      ),
      children: [
        { path: "/", Component: () => <p>início</p> },
        { path: "/conta/trocar-senha", Component: ContaTrocarSenha },
      ],
    },
  ])

  render(<Stub initialEntries={[path]} />)
}

beforeEach(() => vi.resetAllMocks())

describe("rota /conta/trocar-senha", () => {
  it("a sessão com troca obrigatória é mandada para a troca e a vê sem laço", async () => {
    vi.mocked(service.getCurrentUser).mockResolvedValue(me(true))
    renderAt("/")

    expect(
      await screen.findByRole("heading", { name: "Troque a senha inicial" }),
    ).toBeInTheDocument()
  })

  it("trocar a senha libera a sessão restrita sem recarregar a página", async () => {
    vi.mocked(service.getCurrentUser).mockResolvedValue(me(true))
    vi.mocked(service.changePassword).mockResolvedValue(undefined)
    renderAt("/conta/trocar-senha")
    const user = userEvent.setup()
    await screen.findByRole("heading", { name: "Troque a senha inicial" })
    vi.mocked(service.getCurrentUser).mockResolvedValue(me(false))

    await user.type(screen.getByLabelText("Senha atual"), "Provisoria#1")
    await user.type(screen.getByLabelText("Nova senha"), "Dunasolar#2026")
    await user.type(screen.getByLabelText("Repita a senha"), "Dunasolar#2026")
    await user.click(screen.getByRole("button", { name: "Salvar e continuar" }))

    expect(await screen.findByText("início")).toBeInTheDocument()
  })

  it("sem troca obrigatória abre a variante voluntária", async () => {
    vi.mocked(service.getCurrentUser).mockResolvedValue(me(false))
    renderAt("/conta/trocar-senha")

    expect(
      await screen.findByRole("heading", { level: 1, name: "Trocar senha" }),
    ).toBeInTheDocument()
  })
})
