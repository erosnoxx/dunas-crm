import { render, screen } from "@testing-library/react"
import { describe, expect, it } from "vitest"

import { StatusChip } from "./status-chip"

function shapeOf(text: string): string | null | undefined {
  return screen.getByText(text).querySelector("svg")?.getAttribute("data-shape")
}

describe("StatusChip", () => {
  it("cada situação tem uma forma diferente, para ser lida sem cor", () => {
    render(
      <>
        <StatusChip tone="ok">Ativo</StatusChip>
        <StatusChip tone="pending">Convite pendente</StatusChip>
        <StatusChip tone="off">Desativado</StatusChip>
      </>,
    )

    const shapes = [shapeOf("Ativo"), shapeOf("Convite pendente"), shapeOf("Desativado")]

    expect(shapes).toEqual(["filled", "ring", "slashed"])
  })

  it("o círculo, com o traço, cabe na caixa do glifo (o cheio não vira quadrado cortado)", () => {
    render(
      <>
        <StatusChip tone="ok">Ativo</StatusChip>
        <StatusChip tone="pending">Convite pendente</StatusChip>
        <StatusChip tone="off">Desativado</StatusChip>
      </>,
    )

    for (const text of ["Ativo", "Convite pendente", "Desativado"]) {
      const svg = screen.getByText(text).querySelector("svg") as SVGElement
      const box = Number(svg.getAttribute("viewBox")?.split(" ")[2])
      const radius = Number(svg.querySelector("circle")?.getAttribute("r"))
      const stroke = Number(svg.getAttribute("stroke-width"))

      expect(2 * radius + stroke).toBeLessThanOrEqual(box)
    }
  })

  it("a situação normal tem o fundo de chip e as outras têm contorno", () => {
    render(
      <>
        <StatusChip tone="ok">Ativo</StatusChip>
        <StatusChip tone="off">Desativado</StatusChip>
      </>,
    )

    expect(screen.getByText("Ativo")).toHaveClass("bg-chip-ok")
    expect(screen.getByText("Desativado").parentElement).toHaveClass("bg-line-strong")
  })
})
