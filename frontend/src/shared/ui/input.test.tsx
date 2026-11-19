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
})
