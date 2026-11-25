import { render, screen, within } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import { createRoutesStub } from "react-router"
import { afterEach, describe, expect, it, vi } from "vitest"

import type { EquipmentView, LocationView } from "../customers.service"
import { LocationBlock } from "./location-block"

const EQUIPMENT: EquipmentView = {
  id: "e-1",
  serialNumber: "HV2240187563",
  installedOn: "2025-03-12",
  status: "ACTIVE",
  model: { id: "m-5k", name: "SUN2000-5KTL-L1", status: "ACTIVE" },
  manufacturer: { id: "f-huawei", name: "Huawei" },
  type: { id: "t-inv", name: "Inversor" },
  characteristic: { name: "Potência", unit: "KILOWATT", amount: "5.00" },
}

const ACTIVE: LocationView = {
  id: "l-centro",
  name: "Loja Centro",
  status: "ACTIVE",
  address: {
    zipCode: "38400128",
    street: "Av. João Pinheiro",
    number: "1450",
    complement: null,
    neighborhood: "Centro",
    city: "Uberlândia",
    state: "MG",
  },
  equipment: [EQUIPMENT, { ...EQUIPMENT, id: "e-2", status: "DEACTIVATED" }],
}

const DEACTIVATED: LocationView = {
  ...ACTIVE,
  id: "l-fabrica",
  name: "Fábrica",
  status: "DEACTIVATED",
  address: {
    ...ACTIVE.address,
    street: "Rua Tiradentes",
    number: "220",
    complement: "Galpão 2",
  },
  equipment: [EQUIPMENT],
}

const original = window.matchMedia

function setDesktop(matches: boolean) {
  window.matchMedia = ((query: string) => ({
    matches,
    media: query,
    addEventListener: () => undefined,
    removeEventListener: () => undefined,
  })) as unknown as typeof window.matchMedia
}

afterEach(() => {
  window.matchMedia = original
})

type Options = {
  location?: LocationView
  customerActive?: boolean
  canEdit?: boolean
  onReactivate?: () => void
}

async function renderBlock({
  location = ACTIVE,
  customerActive = true,
  canEdit = true,
  onReactivate = () => undefined,
}: Options = {}) {
  const Stub = createRoutesStub([
    {
      path: "/clientes/c-1",
      Component: () => (
        <LocationBlock
          customerId="c-1"
          location={location}
          customerActive={customerActive}
          canEdit={canEdit}
          onReactivate={onReactivate}
          reactivating={false}
        />
      ),
    },
  ])

  render(<Stub initialEntries={["/clientes/c-1"]} />)

  return screen.findByRole("heading", { name: new RegExp(location.name) })
}

describe("LocationBlock, web", () => {
  it("o cartão com título, selo, endereço, contagem dos ativos e a tabela", async () => {
    setDesktop(true)
    await renderBlock({ location: DEACTIVATED })

    expect(screen.getByRole("region", { name: /Fábrica/ })).toBeInTheDocument()
    expect(
      screen.getByText(
        "Rua Tiradentes, 220, Galpão 2 · Centro · Uberlândia/MG · CEP 38400-128",
      ),
    ).toBeInTheDocument()
    expect(
      screen.getByText(/^equipamentos? ativos?$/, { selector: "p" }),
    ).toHaveTextContent("1 equipamento ativo")
    expect(screen.getByRole("table")).toBeInTheDocument()
    expect(document.querySelector("details")).toBeNull()
  })

  it("local ativo: 'Editar local' e 'Novo equipamento' para quem edita", async () => {
    setDesktop(true)
    await renderBlock()

    expect(screen.getByRole("link", { name: "Editar local" })).toHaveAttribute(
      "href",
      "/clientes/c-1/locais/l-centro",
    )
    expect(screen.getByRole("link", { name: "Novo equipamento" })).toHaveAttribute(
      "href",
      "/clientes/c-1/locais/l-centro/equipamentos/novo",
    )
    // Um dos dois equipamentos está desativado: a contagem é só dos ativos.
    expect(
      screen.getByText(/^equipamentos? ativos?$/, { selector: "p" }),
    ).toHaveTextContent("1 equipamento ativo")
  })

  it("cliente desativado: sem 'Novo equipamento', com 'Editar local' (ADR-30)", async () => {
    setDesktop(true)
    await renderBlock({ customerActive: false })

    expect(
      screen.queryByRole("link", { name: "Novo equipamento" }),
    ).not.toBeInTheDocument()
    expect(screen.getByRole("link", { name: "Editar local" })).toBeInTheDocument()
  })

  it("local desativado: o aviso e 'Reativar local', sem 'Editar local' nem 'Novo equipamento'", async () => {
    setDesktop(true)
    const onReactivate = vi.fn()
    await renderBlock({ location: DEACTIVATED, onReactivate })

    expect(
      screen.getByText(/Local desativado: não recebe equipamento novo/),
    ).toBeInTheDocument()
    expect(screen.queryByRole("link", { name: "Editar local" })).not.toBeInTheDocument()
    expect(
      screen.queryByRole("link", { name: "Novo equipamento" }),
    ).not.toBeInTheDocument()

    await userEvent.click(screen.getByRole("button", { name: "Reativar local" }))

    expect(onReactivate).toHaveBeenCalledOnce()
  })

  it("Técnico: nenhuma ação, nem no local desativado", async () => {
    setDesktop(true)
    await renderBlock({ location: DEACTIVATED, canEdit: false })

    expect(screen.queryByRole("link")).not.toBeInTheDocument()
    expect(screen.queryByRole("button")).not.toBeInTheDocument()
  })
})

describe("LocationBlock, celular", () => {
  it("o local ativo é uma seção aberta; o desativado, fechada (ADR-22)", async () => {
    setDesktop(false)
    await renderBlock()
    const active = document.querySelector("details")

    expect(active).toHaveAttribute("open")
    expect(screen.queryByRole("table")).not.toBeInTheDocument()
  })

  it("o local desativado começa fechado, com o aviso e 'Reativar local' dentro", async () => {
    setDesktop(false)
    await renderBlock({ location: DEACTIVATED })
    const details = document.querySelector("details") as HTMLDetailsElement

    expect(details).not.toHaveAttribute("open")
    expect(within(details).getByText(/Local desativado:/)).toBeInTheDocument()
    expect(
      within(details).getByRole("button", { name: "Reativar local", hidden: true }),
    ).toBeInTheDocument()
    expect(
      screen.queryByRole("link", { name: "Editar local", hidden: true }),
    ).not.toBeInTheDocument()
  })

  it("cliente desativado: sem 'Novo equipamento', com 'Editar local'", async () => {
    setDesktop(false)
    await renderBlock({ customerActive: false })

    expect(
      screen.queryByRole("link", { name: "Novo equipamento" }),
    ).not.toBeInTheDocument()
    expect(screen.getByRole("link", { name: "Editar local" })).toBeInTheDocument()
  })

  it("quem edita abre o equipamento pelo cartão inteiro; o Técnico vê o cartão sem link", async () => {
    setDesktop(false)
    await renderBlock()

    expect(
      screen.getAllByRole("link").filter((link) => link.textContent?.includes("Série")),
    ).toHaveLength(2)
    expect(
      screen.getAllByText("Série HV2240187563 · 12/03/2025")[0].closest("a"),
    ).toHaveAttribute("href", "/clientes/c-1/equipamentos/e-1")
  })

  it("Técnico: os cartões de equipamento não são links", async () => {
    setDesktop(false)
    await renderBlock({ canEdit: false })

    expect(screen.getAllByText(/^Série HV2240187563/)).toHaveLength(2)
    expect(screen.queryByRole("link")).not.toBeInTheDocument()
  })
})
