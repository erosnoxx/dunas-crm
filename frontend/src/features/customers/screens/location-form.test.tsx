import { QueryClientProvider } from "@tanstack/react-query"
import { render, screen, waitFor, within } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import { createRoutesStub, Link, Outlet } from "react-router"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"

import { createQueryClient } from "@/shared/lib/query-client"

import * as service from "../customers.service"
import type { CustomerDetail, LocationView } from "../customers.service"
import { LocationFormScreen } from "./location-form"

// O automock esvaziaria `UFS` (a lista do select e do schema): só as funções de rede viram mock.
vi.mock("../customers.service", async (importOriginal) => ({
  ...(await importOriginal<typeof import("../customers.service")>()),
  getCustomer: vi.fn(),
  registerLocation: vi.fn(),
  updateLocation: vi.fn(),
  deactivateLocation: vi.fn(),
  reactivateLocation: vi.fn(),
}))

const FACTORY: LocationView = {
  id: "l1",
  name: "Fábrica",
  status: "ACTIVE",
  address: {
    zipCode: "38446102",
    street: "Rua Tiradentes",
    number: "220",
    complement: "Galpão 2",
    neighborhood: "Distrito Industrial",
    city: "Araguari",
    state: "MG",
  },
  equipment: [],
}

const STORE: LocationView = {
  ...FACTORY,
  id: "l2",
  name: "Loja Centro",
  address: {
    zipCode: "38400128",
    street: "Av. João Pinheiro",
    number: "1450",
    complement: null,
    neighborhood: "Centro",
    city: "Uberlândia",
    state: "MG",
  },
}

const CLOSED: LocationView = {
  ...STORE,
  id: "l3",
  name: "Loja Santa Mônica",
  status: "DEACTIVATED",
}

const SILVA: CustomerDetail = {
  id: "c2",
  document: "34218765000114",
  documentType: "CNPJ",
  name: "J. Silva Comércio de Alimentos Ltda",
  tradeName: "Padaria Boa Vista",
  email: "compras@padariaboavista.com.br",
  phone: "+553432124580",
  status: "ACTIVE",
  locations: [FACTORY, STORE, CLOSED],
}

const withLocation = (location: LocationView): CustomerDetail => ({
  ...SILVA,
  locations: SILVA.locations.map((item) => (item.id === location.id ? location : item)),
})

function setDesktop(matches: boolean) {
  window.matchMedia = ((query: string) => ({
    matches,
    media: query,
    addEventListener: () => undefined,
    removeEventListener: () => undefined,
  })) as unknown as typeof window.matchMedia
}

function renderForm(path: string, canEdit = true, cached?: CustomerDetail) {
  const client = createQueryClient(() => undefined)
  if (cached) client.setQueryData(["customers", "detail", cached.id], cached)
  const Stub = createRoutesStub([
    {
      Component: () => (
        <QueryClientProvider client={client}>
          <Outlet />
        </QueryClientProvider>
      ),
      children: [
        { path: "/clientes/:id", Component: () => <p>ficha do cliente</p> },
        {
          path: "/clientes/:id/locais/novo",
          Component: () => <LocationFormScreen mode="new" canEdit={canEdit} />,
        },
        {
          path: "/clientes/:id/locais/:locationId",
          Component: () => (
            <>
              <LocationFormScreen mode="edit" canEdit={canEdit} />
              <Link to="/clientes/c2/locais/l2">ir para a Loja Centro</Link>
            </>
          ),
        },
      ],
    },
  ])

  render(<Stub initialEntries={[path]} />)

  return userEvent.setup()
}

type User = ReturnType<typeof userEvent.setup>

const originalMatchMedia = window.matchMedia
const field = (label: string) => screen.getByLabelText(label)
const zipCode = () => field("CEP *")
const button = (name: string) => screen.getByRole("button", { name })

async function fillAddress(user: User, overrides: Partial<Record<string, string>> = {}) {
  const values = {
    "Nome do local *": "  Casa ",
    "CEP *": "01310100",
    "Logradouro *": "Av. Paulista",
    "Número *": "S/N",
    "Bairro *": "Bela Vista",
    "Cidade *": "São Paulo",
    ...overrides,
  }

  for (const [label, value] of Object.entries(values))
    if (value) await user.type(await screen.findByLabelText(label), value)
}

beforeEach(() => {
  vi.resetAllMocks()
  vi.mocked(service.getCustomer).mockResolvedValue(SILVA)
})

afterEach(() => {
  window.matchMedia = originalMatchMedia
})

describe("novo local", () => {
  it('CEP "01310-10" mostra "CEP incompleto: informe os 8 dígitos." sob o CEP e não envia', async () => {
    const user = renderForm("/clientes/c2/locais/novo")
    await fillAddress(user, { "CEP *": "0131010" })
    await user.selectOptions(field("UF *"), "SP")
    expect(zipCode()).toHaveValue("01310-10")

    await user.click(button("Salvar local"))

    await waitFor(() =>
      expect(zipCode()).toHaveAccessibleDescription(
        expect.stringContaining("CEP incompleto: informe os 8 dígitos."),
      ),
    )
    expect(zipCode()).toHaveAttribute("aria-invalid", "true")
    expect(zipCode()).toHaveFocus()
    expect(service.registerLocation).not.toHaveBeenCalled()
  })

  it('bairro vazio mostra "Informe o bairro." sob o bairro, com o foco nele', async () => {
    const user = renderForm("/clientes/c2/locais/novo")
    await fillAddress(user, { "Bairro *": "" })
    await user.selectOptions(field("UF *"), "SP")

    await user.click(button("Salvar local"))

    await waitFor(() =>
      expect(field("Bairro *")).toHaveAccessibleDescription("Informe o bairro."),
    )
    expect(field("Bairro *")).toHaveFocus()
    expect(service.registerLocation).not.toHaveBeenCalled()
  })

  it('sem UF: "Escolha a UF." sob a UF, com o foco nela', async () => {
    const user = renderForm("/clientes/c2/locais/novo")
    await fillAddress(user)
    expect(field("UF *")).toHaveValue("")

    await user.click(button("Salvar local"))

    await waitFor(() =>
      expect(field("UF *")).toHaveAccessibleDescription("Escolha a UF."),
    )
    expect(field("UF *")).toHaveFocus()
    expect(service.registerLocation).not.toHaveBeenCalled()
  })

  it("a UF é uma das 27 siglas", async () => {
    renderForm("/clientes/c2/locais/novo")

    const options = within(await screen.findByLabelText("UF *"))
      .getAllByRole("option")
      .map((option) => (option as HTMLOptionElement).value)
      .filter(Boolean)
    expect(options).toEqual([...service.UFS])
  })

  it("envia o CEP com 8 dígitos, o complemento vazio como null e a UF, e volta para a ficha", async () => {
    vi.mocked(service.registerLocation).mockResolvedValue({ id: "l9" })
    const user = renderForm("/clientes/c2/locais/novo")
    await fillAddress(user)
    await user.selectOptions(field("UF *"), "SP")

    await user.click(button("Salvar local"))

    expect(await screen.findByText("ficha do cliente")).toBeInTheDocument()
    expect(service.registerLocation).toHaveBeenCalledWith("c2", {
      name: "Casa",
      address: {
        zipCode: "01310100",
        street: "Av. Paulista",
        number: "S/N",
        complement: null,
        neighborhood: "Bela Vista",
        city: "São Paulo",
        state: "SP",
      },
    })
  })

  it("Salvar fica ocupado enquanto envia: o segundo clique não envia de novo", async () => {
    vi.mocked(service.registerLocation).mockReturnValue(new Promise(() => undefined))
    const user = renderForm("/clientes/c2/locais/novo")
    await fillAddress(user)
    await user.selectOptions(field("UF *"), "SP")

    await user.click(button("Salvar local"))
    await waitFor(() =>
      expect(button("Salvar local")).toHaveAttribute("aria-busy", "true"),
    )
    await user.click(button("Salvar local"))

    expect(service.registerLocation).toHaveBeenCalledTimes(1)
  })

  it("os campos respeitam os limites do domínio, o CEP tem máscara e teclado numérico", async () => {
    const user = renderForm("/clientes/c2/locais/novo")
    await user.type(await screen.findByLabelText("CEP *"), "38446102999")

    expect(zipCode()).toHaveValue("38446-102")
    expect(zipCode()).toHaveAttribute("inputmode", "numeric")
    expect(zipCode()).toHaveAttribute("placeholder", "00000-000")
    expect(field("Nome do local *")).toHaveAttribute("maxlength", "60")
    expect(field("Logradouro *")).toHaveAttribute("maxlength", "150")
    expect(field("Número *")).toHaveAttribute("maxlength", "15")
    expect(field("Complemento")).toHaveAttribute("maxlength", "50")
    expect(field("Bairro *")).toHaveAttribute("maxlength", "100")
    expect(field("Cidade *")).toHaveAttribute("maxlength", "100")
  })

  it.each([
    ["customer_deactivated", 422, "Este cliente está desativado."],
    [
      "concurrent_update",
      409,
      "O registro foi alterado ao mesmo tempo por outra operação; tente de novo.",
    ],
    [undefined, 400, "Cep deve ter entre 8 e 8 caracteres."],
  ])("%s sai no aviso, sem perder o que foi digitado", async (code, status, text) => {
    vi.mocked(service.registerLocation).mockRejectedValue({
      status,
      code,
      message: "cep deve ter entre 8 e 8 caracteres",
    })
    const user = renderForm("/clientes/c2/locais/novo")
    await fillAddress(user)
    await user.selectOptions(field("UF *"), "SP")

    await user.click(button("Salvar local"))

    expect(await screen.findByRole("alert")).toHaveTextContent(text)
    expect(field("Logradouro *")).toHaveValue("Av. Paulista")
    expect(field("Logradouro *")).not.toHaveAttribute("aria-invalid")
  })

  it("o formulário só aparece com a ficha lida agora; antes disso não há Salvar", async () => {
    vi.mocked(service.getCustomer).mockReturnValue(new Promise(() => undefined))
    renderForm("/clientes/c2/locais/novo", true, SILVA)

    expect(await screen.findByRole("status")).toHaveAttribute("aria-busy", "true")
    expect(screen.queryByRole("textbox")).not.toBeInTheDocument()
    expect(screen.queryByRole("button", { name: "Salvar local" })).not.toBeInTheDocument()
  })

  it("cliente desativado volta para a ficha", async () => {
    vi.mocked(service.getCustomer).mockResolvedValue({ ...SILVA, status: "DEACTIVATED" })
    renderForm("/clientes/c2/locais/novo")

    expect(await screen.findByText("ficha do cliente")).toBeInTheDocument()
  })

  it("Técnico: o novo local volta para a ficha", async () => {
    renderForm("/clientes/c2/locais/novo", false)

    expect(await screen.findByText("ficha do cliente")).toBeInTheDocument()
    expect(service.registerLocation).not.toHaveBeenCalled()
  })

  it("celular: cabeçalho próprio, o cliente no texto, Salvar no rodapé e as dicas", async () => {
    renderForm("/clientes/c2/locais/novo")

    expect(
      await screen.findByText(
        "Cliente: J. Silva Comércio de Alimentos Ltda (Padaria Boa Vista)",
      ),
    ).toBeInTheDocument()
    expect(screen.getByRole("heading", { level: 1, name: "Novo local" })).toHaveClass(
      "text-title",
    )
    expect(screen.getByRole("link", { name: "Voltar" })).toHaveAttribute(
      "href",
      "/clientes/c2",
    )
    expect(button("Salvar local")).toHaveClass("min-h-12", "w-full")
    expect(screen.queryByRole("link", { name: "Cancelar" })).not.toBeInTheDocument()
    expect(field("Complemento")).toHaveAttribute("placeholder", "Opcional")
    expect(zipCode()).toHaveAccessibleDescription(
      "O CEP não preenche o endereço: digite todos os campos.",
    )
    expect(field("Número *")).toHaveAccessibleDescription('Sem número, use "S/N".')
  })

  it("web: título display, link com o nome do cliente, Cliente só leitura e Cancelar", async () => {
    setDesktop(true)
    renderForm("/clientes/c2/locais/novo")

    expect(
      await screen.findByText("J. Silva Comércio de Alimentos Ltda (Padaria Boa Vista)"),
    ).toBeInTheDocument()
    expect(screen.getByRole("heading", { level: 1, name: "Novo local" })).toHaveClass(
      "text-display-lg",
    )
    expect(
      screen.getByRole("link", { name: "J. Silva Comércio de Alimentos Ltda" }),
    ).toHaveAttribute("href", "/clientes/c2")
    expect(screen.getByRole("link", { name: "Cancelar" })).toHaveAttribute(
      "href",
      "/clientes/c2",
    )
    expect(field("Complemento (opcional)")).not.toHaveAttribute("placeholder")
    expect(field("Nome do local *")).toHaveAccessibleDescription(
      'Um nome curto para achar o local na ficha, como "Casa" ou "Fazenda Boa Vista".',
    )
    expect(button("Salvar local")).toHaveClass("md:font-semibold")
  })
})

describe("editar local", () => {
  it("o formulário só aparece com a ficha lida agora: antes não há campo nem Salvar", async () => {
    vi.mocked(service.getCustomer).mockReturnValue(new Promise(() => undefined))
    renderForm("/clientes/c2/locais/l1", true, SILVA)

    expect(await screen.findByRole("status")).toHaveAttribute("aria-busy", "true")
    expect(screen.queryByRole("textbox")).not.toBeInTheDocument()
    expect(
      screen.queryByRole("button", { name: "Salvar alterações" }),
    ).not.toBeInTheDocument()
    expect(
      screen.queryByRole("button", { name: "Desativar local" }),
    ).not.toBeInTheDocument()
  })

  it("a ficha guardada no cache não preenche o formulário: só a lida agora", async () => {
    renderForm(
      "/clientes/c2/locais/l1",
      true,
      withLocation({ ...FACTORY, name: "Nome de antes" }),
    )

    expect(await screen.findByLabelText("Nome do local *")).toHaveValue("Fábrica")
  })

  it("vem com os valores do próprio local e o nome dele no título", async () => {
    renderForm("/clientes/c2/locais/l1")

    expect(await screen.findByLabelText("Nome do local *")).toHaveValue("Fábrica")
    expect(screen.getByRole("heading", { level: 1, name: "Fábrica" })).toBeInTheDocument()
    expect(zipCode()).toHaveValue("38446-102")
    expect(field("Logradouro *")).toHaveValue("Rua Tiradentes")
    expect(field("Número *")).toHaveValue("220")
    expect(field("Complemento")).toHaveValue("Galpão 2")
    expect(field("Bairro *")).toHaveValue("Distrito Industrial")
    expect(field("Cidade *")).toHaveValue("Araguari")
    expect(field("UF *")).toHaveValue("MG")
    expect(service.getCustomer).toHaveBeenCalledWith("c2")
  })

  it("Salvar alterações manda o PUT com o corpo exato e volta para a ficha", async () => {
    vi.mocked(service.updateLocation).mockResolvedValue()
    const user = renderForm("/clientes/c2/locais/l1")
    await user.clear(await screen.findByLabelText("Complemento"))
    await user.selectOptions(field("UF *"), "SP")

    await user.click(button("Salvar alterações"))

    expect(await screen.findByText("ficha do cliente")).toBeInTheDocument()
    expect(service.updateLocation).toHaveBeenCalledWith("l1", {
      name: "Fábrica",
      address: {
        zipCode: "38446102",
        street: "Rua Tiradentes",
        number: "220",
        complement: null,
        neighborhood: "Distrito Industrial",
        city: "Araguari",
        state: "SP",
      },
    })
  })

  it.each([
    ["location_deactivated", 422, "Este local está desativado."],
    [
      "concurrent_update",
      409,
      "O registro foi alterado ao mesmo tempo por outra operação; tente de novo.",
    ],
  ])("%s ao salvar sai no aviso e o formulário fica", async (code, status, text) => {
    vi.mocked(service.updateLocation).mockRejectedValue({ status, code, message: "x" })
    const user = renderForm("/clientes/c2/locais/l1")
    await user.clear(await screen.findByLabelText("Nome do local *"))
    await user.type(field("Nome do local *"), "Fábrica nova")

    await user.click(button("Salvar alterações"))

    expect(await screen.findByRole("alert")).toHaveTextContent(text)
    expect(field("Nome do local *")).toHaveValue("Fábrica nova")
  })

  it("local de outro cliente ou inexistente: Local não encontrado, sem formulário", async () => {
    renderForm("/clientes/c2/locais/outro")

    expect(await screen.findByRole("alert")).toHaveTextContent("Local não encontrado.")
    expect(screen.queryByRole("textbox")).not.toBeInTheDocument()
  })

  it("Técnico: a edição volta para a ficha", async () => {
    renderForm("/clientes/c2/locais/l1", false)

    expect(await screen.findByText("ficha do cliente")).toBeInTheDocument()
  })

  it("o local ativo de cliente desativado se edita e salva (ADR-30)", async () => {
    vi.mocked(service.getCustomer).mockResolvedValue({ ...SILVA, status: "DEACTIVATED" })
    vi.mocked(service.updateLocation).mockResolvedValue()
    const user = renderForm("/clientes/c2/locais/l1")

    expect(await screen.findByLabelText("Nome do local *")).toHaveValue("Fábrica")
    expect(button("Desativar local")).toBeInTheDocument()

    await user.click(button("Salvar alterações"))

    expect(await screen.findByText("ficha do cliente")).toBeInTheDocument()
    expect(service.updateLocation).toHaveBeenCalledWith(
      "l1",
      expect.objectContaining({ name: "Fábrica" }),
    )
  })

  it("o local desativado de cliente desativado se reativa (ADR-30)", async () => {
    vi.mocked(service.getCustomer).mockResolvedValue({ ...SILVA, status: "DEACTIVATED" })
    vi.mocked(service.reactivateLocation).mockResolvedValue()
    const user = renderForm("/clientes/c2/locais/l3")

    await user.click(await screen.findByRole("button", { name: "Reativar local" }))

    await waitFor(() =>
      expect(service.reactivateLocation).toHaveBeenCalledWith("l3", expect.anything()),
    )
  })

  it("trocar de local recomeça o formulário com os valores do outro", async () => {
    const user = renderForm("/clientes/c2/locais/l1")
    await user.type(await screen.findByLabelText("Bairro *"), " digitado")

    await user.click(screen.getByRole("link", { name: "ir para a Loja Centro" }))

    await waitFor(() => expect(field("Nome do local *")).toHaveValue("Loja Centro"))
    expect(field("Bairro *")).toHaveValue("Centro")
    expect(field("Complemento")).toHaveValue("")
  })

  it('celular: "Desativar local" sem confirmação troca a página para "Reativar local", sem sair dela', async () => {
    vi.mocked(service.deactivateLocation).mockResolvedValue()
    const user = renderForm("/clientes/c2/locais/l1")
    await screen.findByLabelText("Nome do local *")
    expect(screen.getByText("Ativo")).toBeInTheDocument()
    vi.mocked(service.getCustomer).mockResolvedValue(
      withLocation({ ...FACTORY, status: "DEACTIVATED" }),
    )

    await user.click(button("Desativar local"))

    expect(await screen.findByRole("button", { name: "Reativar local" })).toBeVisible()
    expect(service.deactivateLocation).toHaveBeenCalledWith("l1", expect.anything())
    expect(service.deactivateLocation).toHaveBeenCalledTimes(1)
    // Dentro do <form> no celular: o botão não envia o formulário.
    expect(service.updateLocation).not.toHaveBeenCalled()
    expect(screen.queryByRole("dialog")).not.toBeInTheDocument()
    expect(screen.queryByRole("textbox")).not.toBeInTheDocument()
    expect(screen.queryByText("ficha do cliente")).not.toBeInTheDocument()
    expect(screen.getByText("Desativado")).toBeInTheDocument()
  })

  it("desativar com erro mostra o aviso e o formulário fica", async () => {
    vi.mocked(service.deactivateLocation).mockRejectedValue({
      status: 422,
      code: "location_deactivated",
      message: "x",
    })
    const user = renderForm("/clientes/c2/locais/l1")
    await screen.findByLabelText("Nome do local *")

    await user.click(button("Desativar local"))

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Este local está desativado.",
    )
    expect(field("Nome do local *")).toHaveValue("Fábrica")
  })

  it("web: o painel Situação ao lado, com o selo, a ajuda e Desativar local", async () => {
    setDesktop(true)
    renderForm("/clientes/c2/locais/l1")

    expect(await screen.findByRole("heading", { level: 1, name: "Fábrica" })).toHaveClass(
      "text-display-lg",
    )
    const status = await screen.findByRole("complementary", { name: "Situação do local" })
    expect(within(status).getByText("Ativo")).toBeInTheDocument()
    expect(within(status).getByText(/^Desativar marca o local/)).toBeInTheDocument()
    expect(
      within(status).getByRole("button", { name: "Desativar local" }),
    ).toBeInTheDocument()
    expect(
      screen.getByRole("link", { name: "J. Silva Comércio de Alimentos Ltda" }),
    ).toHaveAttribute("href", "/clientes/c2")
    expect(screen.getByRole("link", { name: "Cancelar" })).toHaveAttribute(
      "href",
      "/clientes/c2",
    )
    expect(button("Salvar alterações")).toBeInTheDocument()
  })
})

describe("local desativado", () => {
  it("aberto por URL fica só leitura, com o aviso e Reativar local", async () => {
    renderForm("/clientes/c2/locais/l3")

    expect(
      await screen.findByRole("button", { name: "Reativar local" }),
    ).toBeInTheDocument()
    expect(screen.queryByRole("textbox")).not.toBeInTheDocument()
    expect(screen.queryByRole("combobox")).not.toBeInTheDocument()
    expect(
      screen.queryByRole("button", { name: "Salvar alterações" }),
    ).not.toBeInTheDocument()
    expect(
      screen.getByRole("heading", { level: 1, name: "Loja Santa Mônica" }),
    ).toBeInTheDocument()
    expect(screen.getByText("Desativado")).toBeInTheDocument()
    expect(screen.getByText("Av. João Pinheiro")).toBeInTheDocument()
    expect(screen.getByText("38400-128")).toBeInTheDocument()
    expect(
      screen.getByText(/^Local desativado: não recebe equipamento novo/),
    ).toBeInTheDocument()
  })

  it("Reativar local volta a página ao formulário, sem sair dela", async () => {
    vi.mocked(service.reactivateLocation).mockResolvedValue()
    const user = renderForm("/clientes/c2/locais/l3")
    await screen.findByRole("button", { name: "Reativar local" })
    vi.mocked(service.getCustomer).mockResolvedValue(
      withLocation({ ...CLOSED, status: "ACTIVE" }),
    )

    await user.click(button("Reativar local"))

    expect(await screen.findByLabelText("Nome do local *")).toHaveValue(
      "Loja Santa Mônica",
    )
    expect(service.reactivateLocation).toHaveBeenCalledWith("l3", expect.anything())
    expect(button("Desativar local")).toBeInTheDocument()
  })

  it("web: os dados à esquerda e Reativar no painel Situação", async () => {
    setDesktop(true)
    renderForm("/clientes/c2/locais/l3")

    const status = await screen.findByRole("complementary", { name: "Situação do local" })
    expect(within(status).getByText("Desativado")).toBeInTheDocument()
    expect(
      within(status).getByRole("button", { name: "Reativar local" }),
    ).toBeInTheDocument()
    expect(screen.getByRole("region", { name: "Dados do local" })).toHaveTextContent(
      "Av. João Pinheiro",
    )
  })

  it("Técnico: o local desativado também volta para a ficha", async () => {
    renderForm("/clientes/c2/locais/l3", false)

    expect(await screen.findByText("ficha do cliente")).toBeInTheDocument()
  })
})
