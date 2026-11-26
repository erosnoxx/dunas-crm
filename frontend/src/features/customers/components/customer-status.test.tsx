import { render, screen } from "@testing-library/react"
import { describe, expect, it } from "vitest"

import { CustomerStatusChip } from "./customer-status"

const shapeOf = (text: string) =>
  screen.getByText(text).querySelector("svg")?.getAttribute("data-shape")

describe("CustomerStatusChip", () => {
  it("lê a situação pela forma: círculo cheio no ativo, vazado no desativado", () => {
    render(
      <>
        <CustomerStatusChip status="ACTIVE" />
        <CustomerStatusChip status="DEACTIVATED" />
      </>,
    )

    expect(shapeOf("Ativo")).toBe("filled")
    expect(shapeOf("Desativado")).toBe("ring")
  })
})
