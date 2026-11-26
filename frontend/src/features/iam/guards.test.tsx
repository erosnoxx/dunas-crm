import { QueryClientProvider } from "@tanstack/react-query"
import { render, screen } from "@testing-library/react"
import { createRoutesStub, Outlet } from "react-router"
import { beforeEach, describe, expect, it, vi } from "vitest"

import { createQueryClient } from "@/shared/lib/query-client"

import {
  RedirectIfAuthenticated,
  RequireAuth,
  RequireRole,
  RequireSettledPassword,
} from "./guards"
import * as service from "./iam.service"
import type { CurrentUser } from "./iam.service"

vi.mock("./iam.service")

const admin: CurrentUser = {
  id: "1",
  name: "Ana",
  email: "a@b.com",
  role: "ADMINISTRATOR",
  mustChangePassword: false,
}
const unauthenticated = { status: 401, code: "session_invalid", message: "sessão" }

function renderAt(path: string) {
  const onSessionInvalid = vi.fn()
  const client = createQueryClient(onSessionInvalid)
  const Stub = createRoutesStub([
    {
      path: "/",
      Component: () => (
        <QueryClientProvider client={client}>
          <Outlet />
        </QueryClientProvider>
      ),
      children: [
        { index: true, Component: () => <p>início</p> },
        {
          path: "entrar",
          Component: () => (
            <RedirectIfAuthenticated>
              <p>tela de entrar</p>
            </RedirectIfAuthenticated>
          ),
        },
        {
          Component: () => (
            <RequireAuth>
              <RequireSettledPassword>
                <Outlet />
              </RequireSettledPassword>
            </RequireAuth>
          ),
          children: [
            { path: "painel", Component: () => <p>painel</p> },
            { path: "conta/trocar-senha", Component: () => <p>trocar senha</p> },
            {
              path: "usuarios",
              Component: () => (
                <RequireRole role="ADMINISTRATOR">
                  <p>usuários</p>
                </RequireRole>
              ),
            },
          ],
        },
      ],
    },
  ])

  render(<Stub initialEntries={[path]} />)

  return { onSessionInvalid }
}

beforeEach(() => vi.resetAllMocks())

describe("RequireAuth", () => {
  it("sem sessão vai para o login", async () => {
    vi.mocked(service.getCurrentUser).mockRejectedValue(unauthenticated)

    renderAt("/painel")

    expect(await screen.findByText("tela de entrar")).toBeInTheDocument()
  })

  it("com sessão mostra a rota", async () => {
    vi.mocked(service.getCurrentUser).mockResolvedValue(admin)

    renderAt("/painel")

    expect(await screen.findByText("painel")).toBeInTheDocument()
  })

  it("não mostra a rota nem redireciona enquanto a sessão carrega", () => {
    vi.mocked(service.getCurrentUser).mockReturnValue(new Promise(() => undefined))

    renderAt("/painel")

    expect(screen.queryByText("painel")).not.toBeInTheDocument()
    expect(screen.queryByText("tela de entrar")).not.toBeInTheDocument()
  })

  it("falha de rede não manda para o login", async () => {
    vi.mocked(service.getCurrentUser).mockRejectedValue({ status: 0, message: "rede" })

    renderAt("/painel")

    await vi.waitFor(() => expect(service.getCurrentUser).toHaveBeenCalledTimes(2), {
      timeout: 3000,
    })
    expect(screen.queryByText("tela de entrar")).not.toBeInTheDocument()
  })
})

describe("RequireRole", () => {
  it("deixa o Administrador abrir /usuarios", async () => {
    vi.mocked(service.getCurrentUser).mockResolvedValue(admin)

    renderAt("/usuarios")

    expect(await screen.findByText("usuários")).toBeInTheDocument()
  })

  it("manda o Comercial de /usuarios para /", async () => {
    vi.mocked(service.getCurrentUser).mockResolvedValue({ ...admin, role: "COMMERCIAL" })

    renderAt("/usuarios")

    expect(await screen.findByText("início")).toBeInTheDocument()
    expect(screen.queryByText("usuários")).not.toBeInTheDocument()
  })
})

describe("RequireSettledPassword", () => {
  it("conta com troca obrigatória abre /usuarios por URL e cai na troca de senha, e fica", async () => {
    vi.mocked(service.getCurrentUser).mockResolvedValue({
      ...admin,
      mustChangePassword: true,
    })

    renderAt("/usuarios")

    expect(await screen.findByText("trocar senha")).toBeInTheDocument()
    await new Promise((resolve) => setTimeout(resolve, 100))
    expect(screen.getByText("trocar senha")).toBeInTheDocument()
    expect(screen.queryByText("usuários")).not.toBeInTheDocument()
  })

  it("conta sem troca obrigatória abre a troca de senha normalmente", async () => {
    vi.mocked(service.getCurrentUser).mockResolvedValue(admin)

    renderAt("/conta/trocar-senha")

    expect(await screen.findByText("trocar senha")).toBeInTheDocument()
  })
})

describe("RedirectIfAuthenticated", () => {
  it("no login sem sessão mostra a tela, sem redirecionar nem tratar a queda como sessão perdida", async () => {
    vi.mocked(service.getCurrentUser).mockRejectedValue(unauthenticated)

    const { onSessionInvalid } = renderAt("/entrar")

    expect(await screen.findByText("tela de entrar")).toBeInTheDocument()
    await vi.waitFor(() => expect(service.getCurrentUser).toHaveBeenCalled())
    expect(onSessionInvalid).not.toHaveBeenCalled()
    expect(screen.getByText("tela de entrar")).toBeInTheDocument()
  })

  it("no login, quem já entrou vai para /", async () => {
    vi.mocked(service.getCurrentUser).mockResolvedValue(admin)

    renderAt("/entrar")

    expect(await screen.findByText("início")).toBeInTheDocument()
  })
})
