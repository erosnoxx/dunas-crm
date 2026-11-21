import { render, screen } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import { useState } from "react"
import { describe, expect, it } from "vitest"

import type { EquipmentTypeSummary } from "../catalog.service"
import { ModelValuesFields } from "./model-values-fields"

const INVERTER: EquipmentTypeSummary = {
  id: "t-inverter",
  name: "Inversor",
  status: "ACTIVE",
  modelCount: 0,
  characteristics: [
    { id: "c-power", name: "Potência", unit: "KILOWATT", required: true },
  ],
}

const AIR_CONDITIONER: EquipmentTypeSummary = {
  id: "t-ac",
  name: "Ar-condicionado",
  status: "ACTIVE",
  modelCount: 0,
  characteristics: [
    { id: "c-capacity", name: "Capacidade", unit: "BTU_PER_HOUR", required: true },
  ],
}

function Harness({
  type,
  initial = "",
  error,
}: {
  type?: EquipmentTypeSummary
  initial?: string
  error?: string
}) {
  const [value, setValue] = useState(initial)

  return <ModelValuesFields type={type} value={value} onChange={setValue} error={error} />
}

describe("ModelValuesFields", () => {
  it("inversor: seção do tipo, Potência * e o sufixo kW", () => {
    render(<Harness type={INVERTER} />)

    expect(
      screen.getByRole("heading", { name: "Características do tipo Inversor" }),
    ).toBeInTheDocument()
    expect(screen.getByLabelText("Potência *")).toBeInTheDocument()
    expect(screen.getByText("kW")).toBeInTheDocument()
    expect(
      screen.getByText(/O campo e a unidade vêm do tipo escolhido/),
    ).toBeInTheDocument()
  })

  it("ar-condicionado: Capacidade * e o sufixo BTU/h", () => {
    render(<Harness type={AIR_CONDITIONER} />)

    expect(screen.getByLabelText("Capacidade *")).toBeInTheDocument()
    expect(screen.getByText("BTU/h")).toBeInTheDocument()
  })

  it("característica opcional não leva asterisco", () => {
    render(
      <Harness
        type={{
          ...INVERTER,
          characteristics: [{ ...INVERTER.characteristics[0], required: false }],
        }}
      />,
    )

    expect(screen.getByLabelText("Potência")).toBeInTheDocument()
  })

  it("sem tipo escolhido não mostra a seção", () => {
    render(<Harness />)

    expect(screen.queryByRole("heading")).not.toBeInTheDocument()
  })

  it("digitar chama onChange e o valor volta pelo value", async () => {
    render(<Harness type={INVERTER} />)

    await userEvent.setup().type(screen.getByLabelText("Potência *"), "3,5")

    expect(screen.getByLabelText("Potência *")).toHaveValue("3,5")
  })

  it("abrir com um valor já preenchido o mantém (edição)", () => {
    render(<Harness type={INVERTER} initial="3" />)

    expect(screen.getByLabelText("Potência *")).toHaveValue("3")
  })

  it("o tipo que chega depois da montagem (consulta carregando) não apaga o valor da edição", () => {
    const { rerender } = render(<Harness type={undefined} initial="3" />)

    rerender(<Harness type={INVERTER} initial="3" />)

    expect(screen.getByLabelText("Potência *")).toHaveValue("3")
  })

  it("trocar o tipo zera o valor digitado, que não vaza para o tipo novo", async () => {
    const { rerender } = render(<Harness type={INVERTER} />)
    await userEvent.setup().type(screen.getByLabelText("Potência *"), "3")
    expect(screen.getByLabelText("Potência *")).toHaveValue("3")

    rerender(<Harness type={AIR_CONDITIONER} />)

    expect(screen.getByLabelText("Capacidade *")).toHaveValue("")
  })

  it("o erro aparece ligado ao campo e o marca como inválido", () => {
    render(<Harness type={INVERTER} error="informe a característica obrigatória" />)

    const field = screen.getByLabelText("Potência *")

    expect(field).toHaveAttribute("aria-invalid", "true")
    expect(field).toHaveAccessibleDescription(/informe a característica obrigatória/)
  })
})
