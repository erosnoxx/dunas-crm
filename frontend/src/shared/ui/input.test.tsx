import { render, screen } from "@testing-library/react"
import { describe, expect, it } from "vitest"

import { Input } from "./input"
import { Label } from "./label"

describe("Input", () => {
  it("repassa maxLength e tipo para o campo", () => {
    render(<Input aria-label="Senha" type="password" maxLength={30} />)

    expect(screen.getByLabelText("Senha")).toHaveAttribute("maxlength", "30")
    expect(screen.getByLabelText("Senha")).toHaveAttribute("type", "password")
  })

  it("o rótulo fica associado ao campo", () => {
    render(
      <>
        <Label htmlFor="email">E-mail</Label>
        <Input id="email" />
      </>,
    )

    expect(screen.getByLabelText("E-mail")).toBeInTheDocument()
  })

  it("campo inválido usa o contorno de erro", () => {
    render(<Input aria-label="E-mail" aria-invalid="true" />)

    expect(screen.getByLabelText("E-mail")).toHaveClass("aria-invalid:border-danger")
  })

  it("o tamanho padrão é 52 px no celular e 48 px a partir do md", () => {
    render(<Input aria-label="Nome" />)

    expect(screen.getByLabelText("Nome")).toHaveClass("h-13", "md:h-12", "px-3.5")
  })

  it("o tamanho compacto é 48 px no celular e 44 px a partir do md", () => {
    render(<Input aria-label="Nome" size="compact" />)

    const input = screen.getByLabelText("Nome")

    expect(input).toHaveClass("h-12", "md:h-11", "px-3")
    expect(input).not.toHaveClass("h-13", "md:h-12")
  })

  it("o compacto fica sobre o painel com o fundo ground a partir do md", () => {
    render(<Input aria-label="Nome" size="compact" />)

    expect(screen.getByLabelText("Nome")).toHaveClass("bg-panel", "md:bg-ground")
  })
})
