import { render, screen } from "@testing-library/react"
import { describe, expect, it } from "vitest"

import { Logo } from "./logo"

describe("Logo", () => {
  it("mostra o ícone com o nome da marca", () => {
    render(<Logo size={40} />)

    const logo = screen.getByRole("img", { name: "DUNAS" })

    expect(logo).toHaveAttribute("src", "/logo.jpeg")
    expect(logo).toHaveAttribute("width", "40")
  })
})
