import { QueryClientProvider } from "@tanstack/react-query"
import { render, screen, waitFor, within } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import { createRoutesStub, Outlet } from "react-router"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"

import { createQueryClient } from "@/shared/lib/query-client"

import * as service from "../customers.service"
import type { CustomerDetail, EquipmentView, LocationView } from "../customers.service"
import { CustomerDetailScreen } from "./customer-detail"

vi.mock("../customers.service")

const EQUIPMENT: EquipmentView = {
  id: "e1",
  serialNumber: "HV2240187563",
  installedOn: "2025-03-12",
  status: "ACTIVE",
  model: { id: "m1", name: "SUN2000-5KTL-L1", status: "ACTIVE" },
  manufacturer: { id: "f1", name: "Huawei" },
  type: { id: "t1", name: "Inversor" },
  characteristic: { name: "Potência", unit: "KILOWATT", amount: "5.00" },
}

const location = (
  id: string,
  name: string,
  over: Partial<LocationView> = {},
): LocationView => ({
  id,
  name,
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
  equipment: [{ ...EQUIPMENT, id: `${id}-e` }],
  ...over,
})

// Fora da ordem alfabética de propósito: a tela não reordena o que a API mandou.
const LOCATIONS = [
  location("l1", "Loja Centro"),
  location("l2", "Fábrica"),
  location("l3", "Loja Santa Mônica", { status: "DEACTIVATED" }),
]

const SILVA: CustomerDetail = {
  id: "c2",
  document: "34218765000114",
  documentType: "CNPJ",
  name: "J. Silva Comércio de Alimentos Ltda",
  tradeName: "Padaria Boa Vista",
  email: "compras@padariaboavista.com.br",
  phone: "+553432124580",
  status: "ACTIVE",
  locations: LOCATIONS,
}

const DEACTIVATED: CustomerDetail = { ...SILVA, status: "DEACTIVATED" }

const MARIANA: CustomerDetail = {
  id: "c1",
  document: "26590311854",
  documentType: "CPF",
  name: "Mariana Tavares Lopes",
  tradeName: null,
  email: null,
  phone: "+5519987542210",
  status: "ACTIVE",
  locations: [],
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

function renderDetail(id = "c2", canEdit = true) {
  const client = createQueryClient(() => undefined)
  const Stub = createRoutesStub([
    {
      Component: () => (
        <QueryClientProvider client={client}>
          <Outlet />
        </QueryClientProvider>
      ),
      children: [
        {
          path: "/clientes/:id",
          Component: () => <CustomerDetailScreen canEdit={canEdit} />,
        },
      ],
    },
  ])

  render(<Stub initialEntries={[`/clientes/${id}`]} />)

  return userEvent.setup()
}

const loaded = (name = SILVA.name) => screen.findByRole("heading", { name })
const link = (name: string) => screen.queryByRole("link", { name, hidden: true })
const button = (name: string) => screen.queryByRole("button", { name, hidden: true })

beforeEach(() => {
  vi.resetAllMocks()
  vi.mocked(service.getCustomer).mockResolvedValue(SILVA)
})

afterEach(() => {
  window.matchMedia = original
})

describe("ficha, web", () => {
  beforeEach(() => setDesktop(true))

  it("cabeçalho, dados e os locais na ordem da API, com o resumo", async () => {
    renderDetail()

    expect(await loaded()).toHaveClass("text-display-lg")
    expect(screen.getByRole("heading", { level: 1 })).toHaveTextContent(SILVA.name)
    expect(screen.getByText("Padaria Boa Vista")).toBeInTheDocument()
    const data = screen.getByRole("region", { name: "Dados do cliente" })
    expect(within(data).getByText("CNPJ")).toBeInTheDocument()
    expect(within(data).getByText("34.218.765/0001-14")).toBeInTheDocument()
    expect(within(data).getByText("Não pode ser alterado.")).toBeInTheDocument()
    expect(within(data).getByText("compras@padariaboavista.com.br")).toBeInTheDocument()
    expect(within(data).getByText("(34) 3212-4580")).toBeInTheDocument()
    expect(screen.getByRole("heading", { level: 2, name: "Locais" })).toBeInTheDocument()
    expect(screen.getByText("2 ativos, 1 desativado")).toBeInTheDocument()
    expect(
      screen.getAllByRole("heading", { level: 3 }).map((h) => h.textContent),
    ).toEqual([
      expect.stringContaining("Loja Centro"),
      expect.stringContaining("Fábrica"),
      expect.stringContaining("Loja Santa Mônica"),
    ])
    expect(service.getCustomer).toHaveBeenCalledWith("c2")
  })

  it("cliente ativo, quem edita: Editar cliente, Novo local e os links do local", async () => {
    renderDetail()
    await loaded()

    expect(link("Clientes")).toHaveAttribute("href", "/clientes")
    expect(link("Editar cliente")).toHaveAttribute("href", "/clientes/c2/editar")
    expect(link("Novo local")).toHaveAttribute("href", "/clientes/c2/locais/novo")
    expect(screen.getAllByRole("link", { name: "Editar local" })[0]).toHaveAttribute(
      "href",
      "/clientes/c2/locais/l1",
    )
    expect(screen.getAllByRole("link", { name: "Novo equipamento" })).toHaveLength(2)
    expect(button("Desativar cliente")).toBeInTheDocument()
    expect(button("Reativar cliente")).not.toBeInTheDocument()
  })

  it("Desativar cliente chama o serviço sem confirmar e a ficha passa a Reativar cliente", async () => {
    vi.mocked(service.getCustomer)
      .mockResolvedValueOnce(SILVA)
      .mockResolvedValue(DEACTIVATED)
    vi.mocked(service.deactivateCustomer).mockResolvedValue()
    const user = renderDetail()
    await loaded()

    await user.click(screen.getByRole("button", { name: "Desativar cliente" }))

    expect(
      await screen.findByRole("button", { name: "Reativar cliente" }),
    ).toBeInTheDocument()
    expect(service.deactivateCustomer).toHaveBeenCalledWith("c2", expect.anything())
    expect(service.deactivateCustomer).toHaveBeenCalledOnce()
    expect(screen.queryByRole("dialog")).not.toBeInTheDocument()
    expect(
      screen.getByText(/^Cliente desativado: os dados dele não se editam/),
    ).toBeInTheDocument()
    expect(button("Desativar cliente")).not.toBeInTheDocument()
    expect(service.getCustomer).toHaveBeenCalledTimes(2)
  })

  it("cliente desativado (ADR-30): sem Editar cliente, Desativar, Novo local e Novo equipamento; com Editar local e Editar", async () => {
    vi.mocked(service.getCustomer).mockResolvedValue(DEACTIVATED)
    renderDetail()
    await loaded()

    expect(link("Editar cliente")).not.toBeInTheDocument()
    expect(button("Desativar cliente")).not.toBeInTheDocument()
    expect(link("Novo local")).not.toBeInTheDocument()
    expect(link("Novo equipamento")).not.toBeInTheDocument()
    expect(screen.queryByText("Não pode ser alterado.")).not.toBeInTheDocument()
    // Os dois locais ativos continuam editáveis; o desativado, não.
    expect(screen.getAllByRole("link", { name: "Editar local" })).toHaveLength(2)
    expect(screen.getAllByRole("link", { name: "Editar" })).toHaveLength(3)
    expect(screen.getByRole("button", { name: "Reativar local" })).toBeInTheDocument()
    const santaMonica = screen.getByRole("region", { name: /Loja Santa Mônica/ })
    expect(
      within(santaMonica).queryByRole("link", { name: "Editar local" }),
    ).not.toBeInTheDocument()
  })

  it("Reativar cliente chama o serviço", async () => {
    vi.mocked(service.getCustomer).mockResolvedValue(DEACTIVATED)
    vi.mocked(service.reactivateCustomer).mockResolvedValue()
    const user = renderDetail()
    await loaded()

    await user.click(screen.getByRole("button", { name: "Reativar cliente" }))

    await waitFor(() =>
      expect(service.reactivateCustomer).toHaveBeenCalledWith("c2", expect.anything()),
    )
  })

  it("Reativar local chama o serviço com o local", async () => {
    vi.mocked(service.reactivateLocation).mockResolvedValue()
    const user = renderDetail()
    await loaded()

    await user.click(screen.getByRole("button", { name: "Reativar local" }))

    await waitFor(() =>
      expect(service.reactivateLocation).toHaveBeenCalledWith("l3", expect.anything()),
    )
  })

  it.each([
    ["location_not_deactivated", "Este local não está desativado."],
    [
      "concurrent_update",
      "O registro foi alterado ao mesmo tempo por outra operação; tente de novo.",
    ],
  ])(
    "%s ao reativar local sai no aviso da ficha, que se recarrega",
    async (code, text) => {
      vi.mocked(service.reactivateLocation).mockRejectedValue({ status: 409, code })
      const user = renderDetail()
      await loaded()

      await user.click(screen.getByRole("button", { name: "Reativar local" }))

      expect(await screen.findByRole("alert")).toHaveTextContent(text)
      await waitFor(() => expect(service.getCustomer).toHaveBeenCalledTimes(2))
    },
  )

  it("customer_deactivated ao desativar sai no aviso da ficha, que se recarrega", async () => {
    vi.mocked(service.deactivateCustomer).mockRejectedValue({
      status: 422,
      code: "customer_deactivated",
    })
    const user = renderDetail()
    await loaded()

    await user.click(screen.getByRole("button", { name: "Desativar cliente" }))

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Este cliente está desativado.",
    )
    await waitFor(() => expect(service.getCustomer).toHaveBeenCalledTimes(2))
  })

  it("sem locais: o vazio, Cadastrar o primeiro local e Não informado no ausente", async () => {
    vi.mocked(service.getCustomer).mockResolvedValue(MARIANA)
    renderDetail("c1")
    await loaded(MARIANA.name)

    expect(screen.getByText("Nenhum local cadastrado")).toBeInTheDocument()
    expect(
      screen.getByText(/^Cadastre o endereço onde há instalação/),
    ).toBeInTheDocument()
    expect(link("Cadastrar o primeiro local")).toHaveAttribute(
      "href",
      "/clientes/c1/locais/novo",
    )
    expect(screen.getByText("Não informado")).toBeInTheDocument()
    expect(screen.queryByText(/ativos?$/)).not.toBeInTheDocument()
  })

  it("erro de carregamento: o aviso da tela 6 e Tentar de novo recarrega", async () => {
    vi.mocked(service.getCustomer).mockRejectedValue({ status: 0, code: "network" })
    const user = renderDetail()

    const alert = await screen.findByRole("alert")
    expect(alert).toHaveTextContent("Não foi possível carregar o cliente")
    expect(alert).toHaveTextContent(
      "Sem resposta do servidor. Confira a conexão e tente de novo.",
    )

    vi.mocked(service.getCustomer).mockResolvedValue(SILVA)
    await user.click(screen.getByRole("button", { name: "Tentar de novo" }))

    expect(await loaded()).toBeInTheDocument()
  })

  it("customer_not_found sai no aviso, sem Tentar de novo", async () => {
    vi.mocked(service.getCustomer).mockRejectedValue({
      status: 404,
      code: "customer_not_found",
      message: "x",
    })
    renderDetail()

    expect(await screen.findByRole("alert")).toHaveTextContent("Cliente não encontrado.")
    expect(button("Tentar de novo")).not.toBeInTheDocument()
    expect(link("Clientes")).toHaveAttribute("href", "/clientes")
  })
})

describe("ficha, celular", () => {
  it("cabeçalho próprio, Editar e Desativar no painel e Novo local fixo embaixo", async () => {
    renderDetail()
    await loaded()

    expect(screen.getByRole("heading", { level: 1, name: "Cliente" })).toBeInTheDocument()
    expect(link("Voltar")).toHaveAttribute("href", "/clientes")
    const data = screen.getByRole("region", { name: "Dados do cliente" })
    expect(within(data).getByRole("link", { name: "Editar cliente" })).toHaveAttribute(
      "href",
      "/clientes/c2/editar",
    )
    expect(
      within(data).getByRole("button", { name: "Desativar cliente" }),
    ).toBeInTheDocument()
    expect(link("Novo local")).toHaveClass("w-full", "min-h-12")
    expect(link("Novo local")).toHaveAttribute("href", "/clientes/c2/locais/novo")
  })

  it("o local ativo começa aberto e o desativado, fechado", async () => {
    renderDetail()
    await loaded()

    const sections = Array.from(document.querySelectorAll("details"))
    expect(sections).toHaveLength(3)
    expect(sections.map((section) => section.open)).toEqual([true, true, false])
  })

  it("sem locais: Cadastrar o primeiro local no lugar de Novo local", async () => {
    vi.mocked(service.getCustomer).mockResolvedValue(MARIANA)
    renderDetail("c1")
    await loaded(MARIANA.name)

    expect(screen.getByText("Nenhum local cadastrado")).toBeInTheDocument()
    expect(link("Cadastrar o primeiro local")).toHaveClass("w-full")
    expect(link("Novo local")).not.toBeInTheDocument()
  })

  it("cliente desativado: Reativar cliente embaixo, sem Novo local nem Novo equipamento, com Editar local", async () => {
    vi.mocked(service.getCustomer).mockResolvedValue(DEACTIVATED)
    renderDetail()
    await loaded()

    expect(screen.getByRole("button", { name: "Reativar cliente" })).toHaveClass("w-full")
    expect(link("Novo local")).not.toBeInTheDocument()
    expect(link("Novo equipamento")).not.toBeInTheDocument()
    expect(link("Editar cliente")).not.toBeInTheDocument()
    expect(button("Desativar cliente")).not.toBeInTheDocument()
    expect(screen.getAllByRole("link", { name: "Editar local" })).toHaveLength(2)
  })
})

describe("ficha, Técnico", () => {
  const ROLE = /^Seu papel \(Técnico\) consulta os clientes/

  it.each([
    ["web", true, SILVA],
    ["web", true, DEACTIVATED],
    ["web", true, MARIANA],
    ["celular", false, SILVA],
    ["celular", false, DEACTIVATED],
    ["celular", false, MARIANA],
  ])("%s: nenhum botão nem link de escrita (%#)", async (_, desktop, customer) => {
    setDesktop(desktop)
    vi.mocked(service.getCustomer).mockResolvedValue(customer)
    renderDetail(customer.id, false)
    await loaded(customer.name)

    expect(screen.getByText(ROLE)).toBeInTheDocument()
    expect(screen.queryAllByRole("button", { hidden: true })).toEqual([])
    // Só o link de volta para a lista: as linhas de equipamento não são links.
    expect(
      screen.getAllByRole("link", { hidden: true }).map((a) => a.getAttribute("href")),
    ).toEqual(["/clientes"])
    expect(screen.queryByText("Não pode ser alterado.")).not.toBeInTheDocument()
    expect(screen.queryByText(/^Cliente desativado:/)).not.toBeInTheDocument()
    expect(screen.queryByText(/^Cadastre o endereço/)).not.toBeInTheDocument()
  })
})
