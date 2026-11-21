import { render, screen } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import { describe, expect, it } from "vitest"

import { Label } from "./label"
import { NativeSelect } from "./native-select"

describe("NativeSelect", () => {
  it("o rótulo fica associado ao campo e as opções aparecem", () => {
    render(
      <>
        <Label htmlFor="tipo">Tipo *</Label>
        <NativeSelect id="tipo" defaultValue="b">
          <option value="a">Inversor</option>
          <option value="b">Ar-condicionado</option>
        </NativeSelect>
      </>,
    )

    expect(screen.getByLabelText("Tipo *")).toHaveValue("b")
    expect(screen.getAllByRole("option")).toHaveLength(2)
  })

  it("campo inválido usa o contorno de erro", () => {
    render(<NativeSelect aria-label="Tipo" aria-invalid="true" />)

    expect(screen.getByLabelText("Tipo")).toHaveClass("aria-invalid:border-danger")
  })

  it("recebe o foco pelo Tab", async () => {
    render(<NativeSelect aria-label="Tipo" />)

    await userEvent.setup().tab()

    expect(screen.getByLabelText("Tipo")).toHaveFocus()
  })

  it("o tamanho compacto é 48 px no celular e 44 px a partir do md", () => {
    render(<NativeSelect aria-label="Tipo" size="compact" />)

    expect(screen.getByLabelText("Tipo")).toHaveClass("h-12", "md:h-11")
  })
})
