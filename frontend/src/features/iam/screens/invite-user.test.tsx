import { QueryClientProvider } from "@tanstack/react-query"
import { render, screen, waitFor } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import { createRoutesStub, Link, Outlet } from "react-router"
import { toast } from "sonner"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"

import { createQueryClient } from "@/shared/lib/query-client"

import * as service from "../iam.service"
import { InviteUserScreen } from "./invite-user"

vi.mock("../iam.service")
vi.mock("sonner", () => ({ toast: { success: vi.fn() } }))

function setDesktop(matches: boolean) {
  window.matchMedia = ((query: string) => ({
    matches,
    media: query,
    addEventListener: () => undefined,
    removeEventListener: () => undefined,
  })) as unknown as typeof window.matchMedia
}

function renderInvite() {
  const client = createQueryClient(() => undefined)
  const Stub = createRoutesStub([
    {
      Component: () => (
        <QueryClientProvider client={client}>
          <Outlet />
        </QueryClientProvider>
      ),
      children: [
        {
          path: "/usuarios",
          Component: () => (
            <>
              <Link to="/usuarios/convidar">Convidar usuário</Link>
              <Outlet />
            </>
          ),
          children: [{ path: "convidar", Component: InviteUserScreen }],
        },
      ],
    },
  ])

  render(<Stub initialEntries={["/usuarios"]} />)

  return { user: userEvent.setup(), client }
}

const originalMatchMedia = window.matchMedia
const send = () => screen.getByRole("button", { name: "Enviar convite" })

async function fill(user: ReturnType<typeof userEvent.setup>, role = "Comercial") {
  await user.type(await screen.findByLabelText("Nome completo"), "Renata Costa")
  await user.type(screen.getByLabelText("E-mail"), "renata@exemplo.com.br")
  await user.click(screen.getByRole("radio", { name: role }))
}

beforeEach(() => {
  vi.resetAllMocks()
  setDesktop(true)
})

afterEach(() => {
  window.matchMedia = originalMatchMedia
})

describe("convidar usuário (web, diálogo)", () => {
  it("é um diálogo modal com título, texto e os três campos", async () => {
    const { user } = renderInvite()
    await user.click(screen.getByRole("link", { name: "Convidar usuário" }))

    const dialog = await screen.findByRole("dialog", { name: "Convidar usuário" })

    expect(dialog).toHaveAttribute("aria-modal", "true")
    expect(
      screen.getByText(
        "O convite chega por e-mail e vale por um tempo limitado. A pessoa define a própria senha.",
      ),
    ).toBeVisible()
    expect(screen.getByLabelText("Nome completo")).toBeVisible()
    expect(screen.getByLabelText("E-mail")).toBeVisible()
    expect(screen.getByRole("radiogroup", { name: "Papel" })).toBeVisible()
    expect(screen.getByRole("button", { name: "Cancelar" })).toBeVisible()
  })

  it("Esc fecha e o foco volta ao botão que abriu", async () => {
    const { user } = renderInvite()
    const trigger = screen.getByRole("link", { name: "Convidar usuário" })
    await user.click(trigger)
    await screen.findByRole("dialog")

    await user.keyboard("{Escape}")

    await waitFor(() => expect(screen.queryByRole("dialog")).not.toBeInTheDocument())
    expect(trigger).toHaveFocus()
  })

  it("Cancelar fecha sem enviar nada", async () => {
    const { user } = renderInvite()
    await user.click(screen.getByRole("link", { name: "Convidar usuário" }))
    await user.click(await screen.findByRole("button", { name: "Cancelar" }))

    await waitFor(() => expect(screen.queryByRole("dialog")).not.toBeInTheDocument())
    expect(service.inviteUser).not.toHaveBeenCalled()
  })

  it("o foco fica preso no diálogo", async () => {
    const { user } = renderInvite()
    await user.click(screen.getByRole("link", { name: "Convidar usuário" }))
    const dialog = await screen.findByRole("dialog")

    for (let i = 0; i < 8; i++) {
      await user.tab()
      // Os guardas de foco do Base UI devolvem o foco ao diálogo logo depois do Tab.
      await waitFor(() =>
        expect(dialog).toContainElement(document.activeElement as HTMLElement),
      )
    }
  })

  it("Enviar convite fica desligado até nome, e-mail e papel estarem válidos", async () => {
    const { user } = renderInvite()
    await user.click(screen.getByRole("link", { name: "Convidar usuário" }))
    await screen.findByRole("dialog")

    expect(send()).toBeDisabled()

    await user.type(screen.getByLabelText("Nome completo"), "R")
    await user.type(screen.getByLabelText("E-mail"), "renata@exemplo.com.br")
    await user.click(screen.getByRole("radio", { name: "Técnico" }))
    expect(send()).toBeDisabled()

    await user.type(screen.getByLabelText("Nome completo"), "enata")
    await waitFor(() => expect(send()).toBeEnabled())
  })

  it("e-mail inválido, nome longo e e-mail longo mantêm o botão desligado, com o motivo", async () => {
    const { user } = renderInvite()
    await user.click(screen.getByRole("link", { name: "Convidar usuário" }))
    await fill(user)
    await waitFor(() => expect(send()).toBeEnabled())

    await user.clear(screen.getByLabelText("E-mail"))
    await user.type(screen.getByLabelText("E-mail"), "sem-arroba")
    await waitFor(() => expect(send()).toBeDisabled())
    expect(await screen.findByText("informe um e-mail válido")).toBeVisible()

    await user.clear(screen.getByLabelText("E-mail"))
    await user.click(screen.getByLabelText("E-mail"))
    await user.paste(`${"a".repeat(250)}@x.com`)
    await waitFor(() => expect(send()).toBeDisabled())

    await user.clear(screen.getByLabelText("Nome completo"))
    await user.click(screen.getByLabelText("Nome completo"))
    await user.paste("n".repeat(81))
    expect(await screen.findByText("o nome tem no máximo 80 caracteres")).toBeVisible()
  })

  it("envia nome e e-mail aparados e o papel; sucesso fecha, atualiza a lista e avisa", async () => {
    vi.mocked(service.inviteUser).mockResolvedValue({ id: "x" })
    const { user, client } = renderInvite()
    const invalidate = vi.spyOn(client, "invalidateQueries")
    await user.click(screen.getByRole("link", { name: "Convidar usuário" }))
    await user.type(await screen.findByLabelText("Nome completo"), "  Renata Costa ")
    await user.type(screen.getByLabelText("E-mail"), "renata@exemplo.com.br")
    await user.click(screen.getByRole("radio", { name: "Comercial" }))

    await user.click(await waitFor(() => (expect(send()).toBeEnabled(), send())))

    expect(service.inviteUser).toHaveBeenCalledWith(
      {
        name: "Renata Costa",
        email: "renata@exemplo.com.br",
        role: "COMMERCIAL",
      },
      expect.anything(),
    )
    await waitFor(() => expect(screen.queryByRole("dialog")).not.toBeInTheDocument())
    expect(toast.success).toHaveBeenCalledWith("Convite enviado")
    expect(invalidate).toHaveBeenCalledWith({ queryKey: ["users"] })
  })

  it("e-mail já cadastrado aparece no campo e-mail, e o diálogo segue aberto", async () => {
    vi.mocked(service.inviteUser).mockRejectedValue({
      status: 409,
      code: "email_already_registered",
      message: "x",
    })
    const { user } = renderInvite()
    await user.click(screen.getByRole("link", { name: "Convidar usuário" }))
    await fill(user)
    await waitFor(() => expect(send()).toBeEnabled())

    await user.click(send())

    const email = screen.getByLabelText("E-mail")
    expect(await screen.findByText("já existe um usuário com este e-mail")).toBeVisible()
    expect(email).toHaveAccessibleDescription("já existe um usuário com este e-mail")
    expect(email).toBeInvalid()
    expect(screen.getByRole("dialog")).toBeVisible()
    expect(screen.queryByRole("alert")).not.toBeInTheDocument()
    expect(toast.success).not.toHaveBeenCalled()
  })

  it("outro erro (forbidden) sai no alerta do formulário", async () => {
    vi.mocked(service.inviteUser).mockRejectedValue({
      status: 403,
      code: "forbidden",
      message: "x",
    })
    const { user } = renderInvite()
    await user.click(screen.getByRole("link", { name: "Convidar usuário" }))
    await fill(user)
    await waitFor(() => expect(send()).toBeEnabled())

    await user.click(send())

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Você não tem permissão para isto",
    )
  })

  it("enviar fica em carregamento e não dispara duas vezes", async () => {
    vi.mocked(service.inviteUser).mockReturnValue(new Promise(() => undefined))
    const { user } = renderInvite()
    await user.click(screen.getByRole("link", { name: "Convidar usuário" }))
    await fill(user)
    await waitFor(() => expect(send()).toBeEnabled())

    await user.dblClick(send())

    expect(send()).toHaveAttribute("aria-busy", "true")
    expect(service.inviteUser).toHaveBeenCalledTimes(1)
  })
})

describe("convidar usuário (celular, tela)", () => {
  beforeEach(() => setDesktop(false))

  it("tem cabeçalho próprio com Voltar para a lista e o h1, e não é um diálogo", async () => {
    const { user } = renderInvite()
    await user.click(screen.getByRole("link", { name: "Convidar usuário" }))

    expect(
      await screen.findByRole("heading", { level: 1, name: "Convidar usuário" }),
    ).toBeVisible()
    expect(screen.getByRole("link", { name: "Voltar" })).toHaveAttribute(
      "href",
      "/usuarios",
    )
    expect(screen.queryByRole("dialog")).not.toBeInTheDocument()
    expect(screen.queryByRole("button", { name: "Cancelar" })).not.toBeInTheDocument()
  })

  it("e-mail já cadastrado aparece no campo e-mail", async () => {
    vi.mocked(service.inviteUser).mockRejectedValue({
      status: 409,
      code: "email_already_registered",
      message: "x",
    })
    const { user } = renderInvite()
    await user.click(screen.getByRole("link", { name: "Convidar usuário" }))
    await fill(user, "Técnico")
    await waitFor(() => expect(send()).toBeEnabled())

    await user.click(send())

    expect(screen.getByLabelText("E-mail")).toHaveAccessibleDescription(
      "já existe um usuário com este e-mail",
    )
  })

  it("sucesso volta para a lista e avisa", async () => {
    vi.mocked(service.inviteUser).mockResolvedValue({ id: "x" })
    const { user } = renderInvite()
    await user.click(screen.getByRole("link", { name: "Convidar usuário" }))
    await fill(user)
    await waitFor(() => expect(send()).toBeEnabled())

    await user.click(send())

    await waitFor(() =>
      expect(screen.queryByLabelText("Nome completo")).not.toBeInTheDocument(),
    )
    expect(toast.success).toHaveBeenCalledWith("Convite enviado")
  })
})
