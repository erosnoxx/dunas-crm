import { render, screen } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import { createRoutesStub } from "react-router"
import { describe, expect, it } from "vitest"

import { LinkProblem } from "./link-problem"

function renderProblem(kind: "expired" | "used", source?: "reset") {
  const Stub = createRoutesStub([
    { path: "/convite", Component: () => <LinkProblem kind={kind} source={source} /> },
    { path: "/entrar", Component: () => <p>tela de entrar</p> },
    { path: "/esqueci-senha", Component: () => <p>tela de esqueci a senha</p> },
  ])

  render(<Stub initialEntries={["/convite"]} />)
}

describe("LinkProblem", () => {
  it("expired: relógio, título, texto e o botão Ir para o login", async () => {
    renderProblem("expired")

    const heading = await screen.findByRole("heading", {
      level: 1,
      name: "Este convite expirou",
    })
    const panel = screen.getByRole("region", { name: "Este convite expirou" })
    expect(panel).toContainElement(heading)
    expect(panel).toHaveTextContent(
      "Por segurança, o link do convite deixa de valer depois de um tempo. Peça ao administrador para enviar um novo convite.",
    )
    const clock = panel.querySelector("svg")
    expect(clock).toHaveAttribute("aria-hidden", "true")
    expect(clock).toHaveClass("text-highlight")
    expect(screen.getByRole("link", { name: "Ir para o login" })).toHaveAttribute(
      "href",
      "/entrar",
    )
  })

  it("used: o mesmo desenho com o título e o texto de link usado", async () => {
    renderProblem("used")

    expect(
      await screen.findByRole("heading", {
        level: 1,
        name: "Este convite já foi usado ou foi substituído",
      }),
    ).toBeInTheDocument()
    expect(
      screen.getByText("Peça ao administrador para enviar um novo convite."),
    ).toBeInTheDocument()
    expect(screen.queryByText(/Por segurança/)).not.toBeInTheDocument()
  })

  it("o botão é do contorno, tem alvo de toque de 44 px ou mais e vai para o login", async () => {
    renderProblem("expired")
    const link = await screen.findByRole("link", { name: "Ir para o login" })

    expect(link).toHaveClass("bg-line-strong", "min-h-13")

    await userEvent.setup().click(link)

    expect(await screen.findByText("tela de entrar")).toBeInTheDocument()
  })

  it.each([
    ["expired", "Este link expirou"],
    ["used", "Este link já foi usado ou foi substituído"],
  ] as const)(
    "redefinição, %s: texto de link e o botão Pedir novo link",
    async (kind, title) => {
      renderProblem(kind, "reset")

      expect(
        await screen.findByRole("heading", { level: 1, name: title }),
      ).toBeInTheDocument()
      expect(
        screen.getByText(
          "Por segurança, o link deixa de valer depois de um tempo. Peça um novo na tela de login.",
        ),
      ).toBeInTheDocument()
      expect(screen.queryByText(/convite/i)).not.toBeInTheDocument()
      expect(screen.queryByRole("link", { name: "Ir para o login" })).toBeNull()

      await userEvent.setup().click(screen.getByRole("link", { name: "Pedir novo link" }))

      expect(await screen.findByText("tela de esqueci a senha")).toBeInTheDocument()
    },
  )
})
