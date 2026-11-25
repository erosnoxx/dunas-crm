import { render, screen, within } from "@testing-library/react"
import { createRoutesStub } from "react-router"
import { describe, expect, it } from "vitest"

import type { EquipmentView } from "../customers.service"
import { EquipmentTable } from "./equipment-table"

const SPLIT: EquipmentView = {
  id: "e-split",
  serialNumber: "2207KX4410",
  installedOn: null,
  status: "ACTIVE",
  model: { id: "m-split", name: "Split Inverter 12.000 BTU", status: "ACTIVE" },
  manufacturer: { id: "f-b", name: "Fabricante B" },
  type: { id: "t-ar", name: "Ar-condicionado" },
  characteristic: { name: "Capacidade", unit: "BTU_PER_HOUR", amount: "12000.00" },
}

const OLD: EquipmentView = {
  id: "e-old",
  serialNumber: "HV2130045219",
  installedOn: "2022-11-08",
  status: "DEACTIVATED",
  model: { id: "m-3k", name: "SUN2000-3KTL-L1", status: "ACTIVE" },
  manufacturer: { id: "f-huawei", name: "Huawei" },
  type: { id: "t-inv", name: "Inversor" },
  characteristic: { name: "Potência", unit: "KILOWATT", amount: 3 },
}

async function renderTable(canEdit: boolean) {
  const Stub = createRoutesStub([
    {
      path: "/clientes/c-1",
      Component: () => (
        <EquipmentTable equipment={[SPLIT, OLD]} canEdit={canEdit} customerId="c-1" />
      ),
    },
  ])

  render(<Stub initialEntries={["/clientes/c-1"]} />)

  return screen.findByRole("table")
}

describe("EquipmentTable", () => {
  it("cabeçalho e linhas saem da mesma lista de colunas, com 'Ações' só para o leitor de tela", async () => {
    const table = await renderTable(true)
    const headers = within(table).getAllByRole("columnheader")

    expect(headers.map((header) => header.textContent)).toEqual([
      "Modelo",
      "Fabricante",
      "Tipo e característica",
      "Número de série",
      "Instalado em",
      "Situação",
      "Ações",
    ])
    expect(within(headers[6]).getByText("Ações")).toHaveClass("sr-only")

    const rows = within(table).getAllByRole("row").slice(1)

    expect(rows).toHaveLength(2)
    for (const row of rows) {
      expect(within(row).getAllByRole("cell")).toHaveLength(headers.length)
    }
  })

  it("a linha traz modelo, fabricante, tipo · característica, série, data e situação", async () => {
    const table = await renderTable(true)
    const [, split, old] = within(table).getAllByRole("row")

    expect(
      within(split)
        .getAllByRole("cell")
        .map((cell) => cell.textContent),
    ).toEqual([
      "Split Inverter 12.000 BTU",
      "Fabricante B",
      "Ar-condicionado · 12000 BTU/h",
      "2207KX4410",
      "Sem data",
      "Ativo",
      "Editar",
    ])
    expect(within(old).getByText("08/11/2022")).toBeInTheDocument()
    expect(old).toHaveClass("text-text-muted")
  })

  it("quem edita: 'Editar' no ativo e 'Abrir' no desativado, para a página do equipamento", async () => {
    await renderTable(true)

    expect(screen.getByRole("link", { name: "Editar" })).toHaveAttribute(
      "href",
      "/clientes/c-1/equipamentos/e-split",
    )
    expect(screen.getByRole("link", { name: "Abrir" })).toHaveAttribute(
      "href",
      "/clientes/c-1/equipamentos/e-old",
    )
  })

  it("Técnico: nem 'Editar' nem 'Abrir'", async () => {
    await renderTable(false)

    expect(screen.queryByRole("link")).not.toBeInTheDocument()
  })

  it("a tabela fica dentro da guarda de rolagem horizontal", async () => {
    const table = await renderTable(true)

    expect(table.parentElement).toHaveClass("overflow-x-auto")
  })
})
