import { render, screen, within } from "@testing-library/react"
import { describe, expect, it } from "vitest"

import { PasswordChecklist } from "./password-checklist"

describe("PasswordChecklist", () => {
  it("lista as 5 regras num grupo com nome", () => {
    render(<PasswordChecklist password="" />)

    const list = screen.getByRole("list", { name: "Regras da senha" })
    expect(within(list).getAllByRole("listitem")).toHaveLength(5)
  })

  it("com Dunasolar marca as 3 primeiras e deixa 2 pendentes, pela forma e pelo texto", () => {
    render(<PasswordChecklist password="Dunasolar" />)

    const items = screen.getAllByRole("listitem")
    const shapes = items.map((li) => li.querySelector("svg")?.getAttribute("data-shape"))
    expect(shapes).toEqual(["filled", "filled", "filled", "ring", "ring"])

    expect(items[0]).toHaveTextContent("De 8 a 30 caracteres, atendida")
    expect(items[2]).toHaveTextContent("Uma letra minúscula, atendida")
    expect(items[3]).toHaveTextContent("Um número, ainda não atendida")
    expect(items[4]).toHaveTextContent(
      "Um caractere especial, como ! ou #, ainda não atendida",
    )

    expect(items[0]).toHaveClass("text-text")
    expect(items[3]).toHaveClass("text-text-muted")
  })

  it("o texto para leitor de tela fica fora da vista", () => {
    render(<PasswordChecklist password="" />)

    expect(screen.getAllByText(", ainda não atendida")[0]).toHaveClass("sr-only")
  })

  it("segue o backend: ç é minúscula, € é especial", () => {
    render(<PasswordChecklist password="Açúcar1€" />)

    const shapes = screen
      .getAllByRole("listitem")
      .map((li) => li.querySelector("svg")?.getAttribute("data-shape"))
    expect(shapes).toEqual(["filled", "filled", "filled", "filled", "filled"])
  })
})
