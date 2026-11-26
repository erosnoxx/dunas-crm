import { render, screen } from "@testing-library/react"
import { describe, expect, it } from "vitest"

import { FieldError } from "./field-error"

describe("FieldError", () => {
  it("é o texto do erro com o id que o campo aponta, e o ícone fica fora do leitor de tela", () => {
    render(<FieldError id="cep-error">Informe o CEP.</FieldError>)

    const error = screen.getByText("Informe o CEP.")

    expect(error).toHaveAttribute("id", "cep-error")
    expect(error).toHaveClass("text-body-sm", "text-danger-text")
    expect(error.querySelector("svg")).toHaveAttribute("aria-hidden", "true")
  })
})
