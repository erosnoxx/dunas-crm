import { render, screen } from "@testing-library/react"
import { describe, expect, it } from "vitest"

import { AuthShell } from "./auth-shell"

describe("AuthShell", () => {
  it("mostra o logo, o nome, o título e a descrição", () => {
    render(
      <AuthShell title="Entrar" description="Acesse o sistema.">
        <p>conteúdo</p>
      </AuthShell>,
    )

    expect(screen.getByText("DUNAS")).toBeInTheDocument()
    expect(screen.getByRole("heading", { level: 1, name: "Entrar" })).toBeInTheDocument()
    expect(screen.getByText("Acesse o sistema.")).toBeInTheDocument()
    expect(screen.getByText("conteúdo")).toBeInTheDocument()
  })

  it("o conteúdo fica no painel de corte maior, nomeado pelo título", () => {
    render(
      <AuthShell title="Entrar" description="Acesse o sistema.">
        <p>conteúdo</p>
      </AuthShell>,
    )

    const panel = screen.getByRole("region", { name: "Entrar" })
    expect(panel).toHaveClass("cut-lg", "md:px-9", "md:py-10", "max-w-110")
    expect(panel).toContainElement(screen.getByText("conteúdo"))
  })

  it("no celular ocupa a tela com p-6, sem o painel", () => {
    const { container } = render(
      <AuthShell title="Entrar" description="d">
        <p>x</p>
      </AuthShell>,
    )

    expect(container.firstElementChild).toHaveClass("min-h-dvh", "px-6", "pb-6")
    expect(screen.getByRole("region", { name: "Entrar" })).toHaveClass(
      "max-md:bg-transparent",
    )
  })

  it("o logo é decorativo, porque o nome DUNAS está escrito ao lado", () => {
    const { container } = render(
      <AuthShell title="Entrar" description="d">
        <p>x</p>
      </AuthShell>,
    )

    expect(screen.queryByRole("img", { name: "DUNAS" })).not.toBeInTheDocument()
    expect(container.querySelector("img")).toHaveAttribute("alt", "")
  })

  it("mostra o rodapé quando é dado, fora do painel", () => {
    render(
      <AuthShell title="Entrar" description="d" footer="O acesso é por convite.">
        <p>x</p>
      </AuthShell>,
    )

    const footer = screen.getByText("O acesso é por convite.")
    expect(footer).toBeInTheDocument()
    expect(screen.getByRole("region", { name: "Entrar" })).not.toContainElement(footer)
  })
})
