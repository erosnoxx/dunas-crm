import { render, screen, waitFor } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import { createRoutesStub, Outlet } from "react-router"
import { beforeEach, describe, expect, it, vi } from "vitest"

import { useLogout, useUsers } from "@/features/iam/iam.hooks"
import { RequireAuth, RedirectIfAuthenticated } from "@/features/iam/guards"
import * as service from "@/features/iam/iam.service"

import { Providers } from "./providers"

vi.mock("@/features/iam/iam.service")

const me = {
  id: "1",
  name: "Ana",
  email: "a@b.com",
  role: "ADMINISTRATOR",
  mustChangePassword: false,
} as const
const sessionInvalid = { status: 401, code: "session_invalid", message: "sessão" }

function Users() {
  const users = useUsers({ page: 0, size: 10 })

  return <p>{users.isError ? "erro" : "usuários"}</p>
}

function Header() {
  const logout = useLogout()

  return <button onClick={() => logout.mutate()}>Sair</button>
}

function renderApp(path = "/usuarios") {
  const Stub = createRoutesStub([
    {
      path: "/",
      Component: () => (
        <Providers>
          <Outlet />
        </Providers>
      ),
      children: [
        {
          Component: () => (
            <RequireAuth>
              <Header />
              <Outlet />
            </RequireAuth>
          ),
          children: [{ path: "usuarios", Component: Users }],
        },
        {
          path: "entrar",
          Component: () => (
            <RedirectIfAuthenticated>
              <p>tela de entrar</p>
            </RedirectIfAuthenticated>
          ),
        },
      ],
    },
  ])

  return render(<Stub initialEntries={[path]} />)
}

beforeEach(() => {
  vi.resetAllMocks()
})

describe("sessão que cai no meio do uso", () => {
  it("vai para o login uma vez e fica lá, sem laço", async () => {
    vi.mocked(service.getCurrentUser)
      .mockResolvedValueOnce(me)
      .mockRejectedValue(sessionInvalid)
    vi.mocked(service.listUsers).mockRejectedValue(sessionInvalid)

    renderApp()

    expect(await screen.findByText("tela de entrar")).toBeInTheDocument()
    await new Promise((resolve) => setTimeout(resolve, 100))
    expect(screen.getByText("tela de entrar")).toBeInTheDocument()
    // o cache limpo pode refazer a consulta ainda montada uma vez; a trava impede novo redirecionamento
    expect(vi.mocked(service.listUsers).mock.calls.length).toBeLessThanOrEqual(2)
    // 1 para entrar na rota protegida + 1 do login público; nunca um laço
    expect(vi.mocked(service.getCurrentUser).mock.calls.length).toBeLessThanOrEqual(2)
  })
})

describe("encerrar a sessão", () => {
  it("depois do logout a rota protegida manda para o login", async () => {
    vi.mocked(service.getCurrentUser).mockResolvedValueOnce(me)
    vi.mocked(service.listUsers).mockResolvedValue({
      content: [],
      page: 0,
      size: 10,
      totalElements: 0,
      totalPages: 0,
    })
    vi.mocked(service.logout).mockResolvedValue(undefined)
    renderApp()
    await screen.findByText("usuários")
    vi.mocked(service.getCurrentUser).mockRejectedValue(sessionInvalid)

    await userEvent.click(screen.getByRole("button", { name: "Sair" }))

    await waitFor(() => expect(screen.getByText("tela de entrar")).toBeInTheDocument())
  })
})
