import { render, screen } from "@testing-library/react"
import { describe, expect, it } from "vitest"

import { DocumentText } from "./document-text"

describe("DocumentText", () => {
  it("mostra o tipo à parte e o número formatado", () => {
    render(<DocumentText type="CNPJ" value="34218765000114" />)

    expect(screen.getByText("CNPJ")).toBeInTheDocument()
    expect(screen.getByText("34.218.765/0001-14")).toBeInTheDocument()
  })

  it("na ficha, o tipo é o selo de fundo chip-ok", () => {
    render(<DocumentText type="CPF" value="31845726090" badge />)

    expect(screen.getByText("CPF")).toHaveClass("bg-chip-ok")
    expect(screen.getByText("318.457.260-90")).toBeInTheDocument()
  })
})
