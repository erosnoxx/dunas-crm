import { render, screen } from "@testing-library/react"
import { describe, expect, it } from "vitest"

import { Spinner } from "./spinner"

describe("Spinner", () => {
  it("anuncia o carregamento como status ocupado, com o ícone fora do leitor de tela", () => {
    render(<Spinner />)

    const status = screen.getByRole("status")

    expect(status).toHaveAttribute("aria-label", "Carregando")
    expect(status).toHaveAttribute("aria-busy", "true")
    expect(status.querySelector("svg")).toHaveAttribute("aria-hidden", "true")
  })
})
