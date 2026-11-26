import { render, screen } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import { describe, expect, it } from "vitest"

import { Checkbox } from "./checkbox"

describe("Checkbox", () => {
  it("o rótulo fica associado à caixa e o clique no texto marca", async () => {
    render(<Checkbox label="Obrigatória no cadastro do modelo" />)
    const box = screen.getByLabelText("Obrigatória no cadastro do modelo")

    await userEvent.setup().click(screen.getByText("Obrigatória no cadastro do modelo"))

    expect(box).toBeChecked()
  })

  it("repassa aria-invalid para a caixa", () => {
    render(<Checkbox label="Obrigatória" aria-invalid="true" />)

    expect(screen.getByLabelText("Obrigatória")).toHaveAttribute("aria-invalid", "true")
  })

  it("recebe o foco pelo Tab", async () => {
    render(<Checkbox label="Obrigatória" />)

    await userEvent.setup().tab()

    expect(screen.getByLabelText("Obrigatória")).toHaveFocus()
  })

  it("a caixa usa o laranja da marca e o rótulo tem alvo de toque de 44 px", () => {
    render(<Checkbox label="Obrigatória" />)

    expect(screen.getByLabelText("Obrigatória")).toHaveClass("accent-brand")
    expect(screen.getByText("Obrigatória").closest("label")).toHaveClass("min-h-11")
  })
})
