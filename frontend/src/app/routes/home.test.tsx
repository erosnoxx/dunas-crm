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
    { path: "/clientes", Component: () => <p>tela de clientes</p> },
  ])

  render(<Stub initialEntries={["/"]} />)
}

beforeEach(() => vi.resetAllMocks())

describe("rota /", () => {
  it.each<Role>(["ADMINISTRATOR", "COMMERCIAL", "TECHNICIAN"])(
    "leva %s para os clientes",
    async (role) => {
      renderHome(role)

      expect(await screen.findByText("tela de clientes")).toBeInTheDocument()
    },
  )
})
