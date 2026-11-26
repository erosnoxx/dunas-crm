import { render, screen } from "@testing-library/react"
import { describe, expect, it } from "vitest"

import { Panel } from "./panel"

describe("Panel", () => {
  it("tem a superfície do painel e o corte maior", () => {
    render(<Panel aria-label="Dados">conteúdo</Panel>)

    expect(screen.getByLabelText("Dados")).toHaveClass("bg-panel", "cut-lg")
  })
})
