import { QueryClientProvider } from "@tanstack/react-query"
import { render, screen } from "@testing-library/react"
import { createRoutesStub } from "react-router"
import { beforeEach, describe, expect, it, vi } from "vitest"

import * as service from "@/features/iam/iam.service"
import type { CurrentUser, Role } from "@/features/iam/iam.service"
import { createQueryClient } from "@/shared/lib/query-client"

import Home from "./home"

vi.mock("@/features/iam/iam.service")

const me = (role: Role): CurrentUser => ({
  id: "1",
  name: "Paula Lima",
  email: "paula@dunas.com",
  role,
  mustChangePassword: false,
})

function renderHome(role: Role) {
  vi.mocked(service.getCurrentUser).mockResolvedValue(me(role))
  const client = createQueryClient(() => undefined)
  const Stub = createRoutesStub([
    {
      Component: () => (
        <QueryClientProvider client={client}>
          <Home />
        </QueryClientProvider>
      ),
      path: "/",
    },
    { path: "/usuarios", Component: () => <p>tela de usuários</p> },
  ])

  render(<Stub initialEntries={["/"]} />)
}

beforeEach(() => vi.resetAllMocks())

describe("rota /", () => {
  it("leva o Administrador para os usuários", async () => {
    renderHome("ADMINISTRATOR")

    expect(await screen.findByText("tela de usuários")).toBeInTheDocument()
  })

  it.each<Role>(["COMMERCIAL", "TECHNICIAN"])(
    "%s vê a página inicial provisória com o texto aprovado",
    async (role) => {
      renderHome(role)

      expect(
        await screen.findByRole("heading", { level: 1, name: "Início" }),
      ).toBeInTheDocument()
      expect(screen.getByRole("heading", { name: "Em construção" })).toBeInTheDocument()
      expect(
        screen.getByText(
          "As telas de trabalho chegam com as próximas funcionalidades. Por enquanto, aqui você acessa a sua conta e as suas sessões.",
        ),
      ).toBeInTheDocument()
    },
  )
})
