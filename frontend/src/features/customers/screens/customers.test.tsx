import { QueryClientProvider } from "@tanstack/react-query"
import { render, screen, waitFor, within } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import { createRoutesStub, Outlet, useLocation } from "react-router"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"

import { createQueryClient } from "@/shared/lib/query-client"

import * as service from "../customers.service"
import type { CustomerSummary } from "../customers.service"
import { CustomersScreen } from "./customers"

vi.mock("../customers.service")

const customer = (n: number, over: Partial<CustomerSummary> = {}): CustomerSummary => ({
  id: `c${n}`,
  name: `Cliente ${String(n).padStart(2, "0")}`,
  tradeName: null,
  document: "31845726090",
  documentType: "CPF",
  status: "ACTIVE",
  locationCount: 1,
  equipmentCount: 1,
  cities: [{ city: "Campinas", state: "SP" }],
  matchedSerials: [],
  ...over,
})

const ANA = customer(1, {
  name: "Ana Paula Ribeiro",
  locationCount: 1,
  equipmentCount: 3,
  cities: [{ city: "Ribeirão Preto", state: "SP" }],
})
const SILVA = customer(2, {
  name: "J. Silva Comércio de Alimentos Ltda",
  tradeName: "Padaria Boa Vista",
  document: "34218765000114",
  documentType: "CNPJ",
  locationCount: 2,
  equipmentCount: 5,
  cities: [
    { city: "Araguari", state: "MG" },
    { city: "Uberlândia", state: "MG" },
  ],
})
const HOTEL = customer(3, {
  name: "Hotel Fazenda Serra Azul Ltda",
  tradeName: "Serra Azul",
  document: "51903648000109",
  documentType: "CNPJ",
  status: "DEACTIVATED",
  locationCount: 1,
  equipmentCount: 4,
  cities: [{ city: "Pirenópolis", state: "GO" }],
})

const FIRST = [ANA, SILVA, HOTEL]
const MANY = [...FIRST, ...Array.from({ length: 22 }, (_, i) => customer(i + 4))]

const page = (
  content: CustomerSummary[],
  number = 0,
  size = 20,
  total = content.length,
) => ({
  content,
  page: number,
  size,
  totalElements: total,
  totalPages: Math.ceil(total / size),
})

function serve(all: CustomerSummary[]) {
  vi.mocked(service.listCustomers).mockImplementation(async ({ page: number, size }) =>
    page(all.slice(number * size, (number + 1) * size), number, size, all.length),
  )
}

/** Lista padrão vazia; `deactivated` desativados na consulta do ADR-28. */
function serveEmpty(deactivated: CustomerSummary[]) {
  vi.mocked(service.listCustomers).mockImplementation(async ({ status, size }) =>
    status === "DEACTIVATED"
      ? page(deactivated.slice(0, size), 0, size, deactivated.length)
      : page([], 0, size),
  )
}

function setDesktop(matches: boolean) {
  window.matchMedia = ((query: string) => ({
    matches,
    media: query,
    addEventListener: () => undefined,
    removeEventListener: () => undefined,
  })) as unknown as typeof window.matchMedia
}

function Search() {
  return <span data-testid="search">{useLocation().search}</span>
}

function renderScreen({ canEdit = true, url = "/clientes" } = {}) {
  const client = createQueryClient(() => undefined)
  const Stub = createRoutesStub([
    {
      Component: () => (
        <QueryClientProvider client={client}>
          <Outlet />
          <Search />
        </QueryClientProvider>
      ),
      children: [
        { path: "/clientes", Component: () => <CustomersScreen canEdit={canEdit} /> },
      ],
    },
  ])

  render(<Stub initialEntries={[url]} />)

  return userEvent.setup()
}

const params = () => new URLSearchParams(screen.getByTestId("search").textContent ?? "")
const card = (name: string) => screen.getByText(name).closest("li") as HTMLElement
const row = (name: string) => screen.getByText(name).closest("tr") as HTMLElement
const lastCall = () => vi.mocked(service.listCustomers).mock.lastCall?.[0]

const originalMatchMedia = window.matchMedia

beforeEach(() => {
  vi.resetAllMocks()
  serve(MANY)
})

afterEach(() => {
  window.matchMedia = originalMatchMedia
})

describe("lista (celular)", () => {
  it("pede os ativos e o cartão mostra nome, nome fantasia, documento, cidades, contagens e situação", async () => {
    renderScreen()
    await screen.findByText("J. Silva Comércio de Alimentos Ltda")

    const silva = card("J. Silva Comércio de Alimentos Ltda")
    expect(within(silva).getByText("Padaria Boa Vista")).toBeVisible()
    expect(silva).toHaveTextContent("CNPJ 34.218.765/0001-14")
    expect(within(silva).getByText("Araguari/MG, Uberlândia/MG")).toBeVisible()
    expect(within(silva).getByText("2 locais · 5 equip.")).toBeVisible()
    expect(within(silva).getByText("Ativo")).toBeVisible()
    expect(within(silva).getByRole("link")).toHaveAttribute("href", "/clientes/c2")

    expect(card("Ana Paula Ribeiro")).toHaveTextContent("CPF 318.457.260-90")
    expect(
      within(card("Ana Paula Ribeiro")).getByText("1 local · 3 equip."),
    ).toBeVisible()
    expect(
      within(card("Hotel Fazenda Serra Azul Ltda")).getByText("Desativado"),
    ).toBeVisible()
    expect(service.listCustomers).toHaveBeenCalledWith({
      status: "ACTIVE",
      page: 0,
      size: 20,
    })
  })

  it("busca com o placeholder e a dica do celular (tela 2)", async () => {
    renderScreen()

    const search = await screen.findByLabelText("Buscar cliente")

    expect(search).toHaveAttribute(
      "placeholder",
      "Nome, documento, cidade ou nº de série",
    )
    expect(search).toHaveAccessibleDescription(
      "Também acha pelo número de série do equipamento.",
    )
  })

  it("a série casada aparece abaixo do nome (D1)", async () => {
    serve([
      {
        ...SILVA,
        matchedSerials: [{ serialNumber: "HV9910042", modelName: "SUN2000-5KTL-L1" }],
      },
    ])
    renderScreen({ url: "/clientes?q=HV991" })

    expect(
      await screen.findByText("Série HV9910042 · SUN2000-5KTL-L1"),
    ).toBeInTheDocument()
  })

  it("a cidade buscada vem primeiro", async () => {
    serve([
      customer(9, {
        name: "Transportes Cerrado Ltda",
        cities: [
          { city: "Anápolis", state: "GO" },
          { city: "Goiânia", state: "GO" },
          { city: "Trindade", state: "GO" },
        ],
      }),
    ])
    renderScreen({ url: "/clientes?q=Trindade" })
    await screen.findByText("Transportes Cerrado Ltda")

    expect(
      within(card("Transportes Cerrado Ltda")).getByText("Trindade/GO, Anápolis/GO"),
    ).toBeVisible()
  })

  it('com busca, mostra "N clientes encontrados" (tela 5); sem busca, não', async () => {
    serve([SILVA])
    renderScreen({ url: "/clientes?q=HV2240187563" })

    expect(await screen.findByText("1 cliente encontrado")).toBeVisible()
  })

  it("sem busca, não conta os encontrados", async () => {
    renderScreen()
    await screen.findByText("Ana Paula Ribeiro")

    expect(screen.queryByText(/encontrados?$/)).not.toBeInTheDocument()
  })

  it("Novo cliente é um link para o formulário, fixo acima da barra inferior", async () => {
    renderScreen()

    const link = await screen.findByRole("link", { name: "Novo cliente" })

    expect(link).toHaveAttribute("href", "/clientes/novo")
    expect(link.parentElement).toHaveClass("max-md:fixed", "max-md:bottom-16")
    expect(screen.getByRole("main")).toHaveClass("max-md:pb-22")
  })

  it("Técnico: sem Novo cliente e com o aviso de que o papel só consulta (tela 5)", async () => {
    renderScreen({ canEdit: false })
    await screen.findByText("Ana Paula Ribeiro")

    expect(screen.queryByRole("link", { name: "Novo cliente" })).not.toBeInTheDocument()
    expect(
      screen.getByText(
        "Seu papel (Técnico) consulta os clientes. Quem cadastra e altera é o Administrador e o Comercial.",
      ),
    ).toBeVisible()
    expect(screen.getByRole("main")).not.toHaveClass("max-md:pb-22")
  })

  it('mostra "Mostrando 20 de 25 clientes" e Carregar mais acrescenta a próxima página', async () => {
    const user = renderScreen()
    await screen.findByText("Ana Paula Ribeiro")

    expect(screen.getByText("Mostrando 20 de 25 clientes")).toBeVisible()

    await user.click(screen.getByRole("button", { name: "Carregar mais" }))

    expect(await screen.findByText("Cliente 25")).toBeInTheDocument()
    expect(lastCall()).toEqual({ status: "ACTIVE", page: 1, size: 20 })
    expect(screen.getByText("Ana Paula Ribeiro")).toBeInTheDocument()
  })
})

describe("busca e situação na URL", () => {
  it("a busca chega ao serviço como digitada e à URL", async () => {
    const user = renderScreen()
    await screen.findByText("Ana Paula Ribeiro")

    await user.type(screen.getByLabelText("Buscar cliente"), "34.218.765")

    await waitFor(() =>
      expect(lastCall()).toEqual({
        text: "34.218.765",
        status: "ACTIVE",
        page: 0,
        size: 20,
      }),
    )
    expect(params().get("q")).toBe("34.218.765")
    expect(params().has("situacao")).toBe(false)
  })

  it("as situações vêm na ordem Ativos, Desativados, Todos", async () => {
    renderScreen()

    const select = await screen.findByLabelText("Situação")

    expect(
      within(select)
        .getAllByRole("option")
        .map((o) => o.textContent),
    ).toEqual(["Ativos", "Desativados", "Todos"])
    expect(select).toHaveValue("ACTIVE")
  })

  it.each([
    ["Desativados", "DEACTIVATED"],
    ["Todos", "ALL"],
  ] as const)("situação %s pede %s e vai para a URL", async (label, status) => {
    const user = renderScreen()
    await screen.findByText("Ana Paula Ribeiro")

    await user.selectOptions(screen.getByLabelText("Situação"), label)

    await waitFor(() => expect(lastCall()).toEqual({ status, page: 0, size: 20 }))
    expect(params().get("situacao")).toBe(status)
  })

  it("?q=34218765000114&situacao=ALL abre já buscando (ADR-23)", async () => {
    serve([SILVA])
    renderScreen({ url: "/clientes?q=34218765000114&situacao=ALL" })

    expect(await screen.findByText("J. Silva Comércio de Alimentos Ltda")).toBeVisible()
    expect(screen.getByLabelText("Buscar cliente")).toHaveValue("34218765000114")
    expect(screen.getByLabelText("Situação")).toHaveValue("ALL")
    expect(service.listCustomers).toHaveBeenCalledWith({
      text: "34218765000114",
      status: "ALL",
      page: 0,
      size: 20,
    })
  })

  it("situação desconhecida na URL cai em Ativos", async () => {
    renderScreen({ url: "/clientes?situacao=XYZ" })
    await screen.findByText("Ana Paula Ribeiro")

    expect(screen.getByLabelText("Situação")).toHaveValue("ACTIVE")
    expect(service.listCustomers).toHaveBeenCalledWith({
      status: "ACTIVE",
      page: 0,
      size: 20,
    })
  })
})

describe("vazios", () => {
  it('sem resultado mostra o termo, e "Limpar busca e filtros" refaz a consulta (tela 3)', async () => {
    // A lista padrão fica pendente: o vazio anterior (placeholder) não pode virar o vazio padrão.
    let release!: () => void
    const pending = new Promise<void>((resolve) => (release = resolve))
    vi.mocked(service.listCustomers).mockImplementation(async (input) => {
      if (input.text) return page([])
      await pending
      return page(FIRST)
    })
    const user = renderScreen({ url: "/clientes?q=HV9910042&situacao=DEACTIVATED" })

    expect(
      await screen.findByRole("heading", { name: "Nenhum cliente encontrado" }),
    ).toBeInTheDocument()
    expect(
      screen.getByText(
        'Nenhum cliente desativado tem "HV9910042" no nome, no documento, numa cidade ou no número de série de um equipamento. Confira a busca ou cadastre o cliente.',
      ),
    ).toBeVisible()

    await user.click(screen.getByRole("button", { name: "Limpar busca e filtros" }))

    await waitFor(() =>
      expect(lastCall()).toEqual({ status: "ACTIVE", page: 0, size: 20 }),
    )
    expect(screen.getByRole("status")).toHaveAttribute("aria-busy", "true")
    expect(screen.queryByRole("heading", { level: 2 })).not.toBeInTheDocument()

    release()

    expect(await screen.findByText("Ana Paula Ribeiro")).toBeVisible()
    expect(service.listCustomers).not.toHaveBeenCalledWith(
      expect.objectContaining({ size: 1 }),
    )
    expect(screen.getByLabelText("Buscar cliente")).toHaveValue("")
    expect(screen.getByLabelText("Situação")).toHaveValue("ACTIVE")
    expect(params().toString()).toBe("")
  })

  it("sem resultado entre os ativos sugere ver os desativados (texto da tela 3)", async () => {
    serve([])
    renderScreen({ url: "/clientes?q=HV9910042" })

    expect(
      await screen.findByText(
        'Nenhum cliente ativo tem "HV9910042" no nome, no documento, numa cidade ou no número de série de um equipamento. Confira a busca, veja os desativados ou cadastre o cliente.',
      ),
    ).toBeVisible()
  })

  it("vazio padrão sem desativados: Nenhum cliente cadastrado, com busca e filtro na tela (tela 4)", async () => {
    serveEmpty([])
    renderScreen()

    expect(
      await screen.findByRole("heading", { name: "Nenhum cliente cadastrado" }),
    ).toBeInTheDocument()
    expect(
      screen.getByText(
        "Cadastre o cliente com o CPF ou o CNPJ e um contato. Depois, na ficha dele, cadastre os locais e os equipamentos instalados em cada um.",
      ),
    ).toBeVisible()
    expect(
      screen.getByRole("link", { name: "Cadastrar o primeiro cliente" }),
    ).toHaveAttribute("href", "/clientes/novo")
    expect(screen.getByLabelText("Buscar cliente")).toBeVisible()
    expect(screen.getByLabelText("Situação")).toBeVisible()
    expect(service.listCustomers).toHaveBeenCalledWith({
      status: "DEACTIVATED",
      page: 0,
      size: 1,
    })
  })

  it('falha na contagem dos desativados: o aviso da tela 6, e "Tentar de novo" refaz a contagem', async () => {
    let countFails = true
    vi.mocked(service.listCustomers).mockImplementation(async ({ status, size }) => {
      if (status !== "DEACTIVATED") return page([], 0, size)
      if (countFails) throw { status: 403, code: "forbidden", message: "Forbidden" }
      return page([HOTEL], 0, size)
    })
    const countCalls = () =>
      vi.mocked(service.listCustomers).mock.calls.filter(([input]) => input.size === 1)
        .length
    const user = renderScreen()

    const alert = await screen.findByRole("alert")

    expect(
      within(alert).getByRole("heading", {
        name: "Não foi possível carregar os clientes",
      }),
    ).toBeInTheDocument()
    expect(screen.queryByText("Nenhum cliente cadastrado")).not.toBeInTheDocument()
    expect(
      screen.queryByRole("link", { name: "Cadastrar o primeiro cliente" }),
    ).not.toBeInTheDocument()
    const before = countCalls()
    countFails = false

    await user.click(screen.getByRole("button", { name: "Tentar de novo" }))

    expect(
      await screen.findByRole("heading", { name: "Nenhum cliente ativo" }),
    ).toBeInTheDocument()
    expect(countCalls()).toBe(before + 1)
  })

  it("Técnico no vazio padrão: sem Cadastrar o primeiro cliente", async () => {
    serveEmpty([])
    renderScreen({ canEdit: false })

    await screen.findByRole("heading", { name: "Nenhum cliente cadastrado" })

    expect(
      screen.queryByRole("link", { name: "Cadastrar o primeiro cliente" }),
    ).not.toBeInTheDocument()
  })

  it('vazio padrão com desativados: Nenhum cliente ativo, e "Ver desativados" muda o filtro (D2)', async () => {
    serveEmpty([HOTEL])
    const user = renderScreen()

    expect(
      await screen.findByRole("heading", { name: "Nenhum cliente ativo" }),
    ).toBeInTheDocument()
    expect(
      screen.getByText(
        "Todos os clientes cadastrados estão desativados. Veja os desativados para reativar um deles.",
      ),
    ).toBeVisible()
    expect(screen.getByRole("link", { name: "Novo cliente" })).toBeInTheDocument()

    await user.click(screen.getByRole("button", { name: "Ver desativados" }))

    expect(await screen.findByText("Hotel Fazenda Serra Azul Ltda")).toBeVisible()
    expect(lastCall()).toEqual({ status: "DEACTIVATED", page: 0, size: 20 })
    expect(screen.getByLabelText("Situação")).toHaveValue("DEACTIVATED")
    expect(params().get("situacao")).toBe("DEACTIVATED")
  })

  it("a consulta de desativados não sai quando a lista tem cliente", async () => {
    renderScreen()
    await screen.findByText("Ana Paula Ribeiro")

    expect(service.listCustomers).not.toHaveBeenCalledWith(
      expect.objectContaining({ size: 1 }),
    )
  })
})

describe("estados", () => {
  it("carregando mostra o indicador e nenhum cliente", async () => {
    vi.mocked(service.listCustomers).mockReturnValue(new Promise(() => undefined))
    renderScreen()

    expect(await screen.findByRole("status")).toHaveAttribute("aria-busy", "true")
    expect(screen.queryAllByRole("listitem")).toHaveLength(0)
  })

  it("falha de rede: o aviso da tela 6, com busca, filtro e Novo cliente usáveis", async () => {
    vi.mocked(service.listCustomers).mockRejectedValue({
      status: 0,
      message: "não foi possível conectar ao servidor",
    })
    renderScreen()

    const alert = await screen.findByRole("alert")

    expect(
      within(alert).getByRole("heading", {
        name: "Não foi possível carregar os clientes",
      }),
    ).toBeInTheDocument()
    expect(alert).toHaveTextContent(
      "Sem resposta do servidor. Confira a conexão e tente de novo; a busca e o filtro ficam como estão.",
    )
    expect(screen.getByLabelText("Buscar cliente")).toBeEnabled()
    expect(screen.getByLabelText("Situação")).toBeEnabled()
    expect(screen.getByRole("link", { name: "Novo cliente" })).toBeInTheDocument()
  })

  it('erro com code sai pela mensagem, e "Tentar de novo" refaz a consulta', async () => {
    vi.mocked(service.listCustomers).mockRejectedValueOnce({
      status: 403,
      code: "forbidden",
      message: "Forbidden",
    })
    const user = renderScreen()

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Você não tem permissão para isto.",
    )
    const calls = vi.mocked(service.listCustomers).mock.calls.length

    await user.click(screen.getByRole("button", { name: "Tentar de novo" }))

    expect(await screen.findByText("Ana Paula Ribeiro")).toBeVisible()
    expect(vi.mocked(service.listCustomers).mock.calls.length).toBe(calls + 1)
    expect(screen.queryByRole("alert")).not.toBeInTheDocument()
  })
})

describe("web", () => {
  beforeEach(() => setDesktop(true))

  it("tabela com as colunas da tela 1, cabeçalho e linhas da mesma definição", async () => {
    renderScreen()
    await screen.findByText("Ana Paula Ribeiro")

    const headers = screen.getAllByRole("columnheader")
    expect(headers.map((th) => th.textContent)).toEqual([
      "Cliente",
      "Documento",
      "Cidades",
      "Locais",
      "Equipamentos",
      "Situação",
      "Ações",
    ])
    expect(within(row("Ana Paula Ribeiro")).getAllByRole("cell")).toHaveLength(
      headers.length,
    )
  })

  it("a linha mostra nome, nome fantasia, documento, cidades, contagens, situação e Abrir", async () => {
    renderScreen()
    await screen.findByText("J. Silva Comércio de Alimentos Ltda")

    const silva = row("J. Silva Comércio de Alimentos Ltda")
    const cells = within(silva).getAllByRole("cell")
    expect(cells[0]).toHaveTextContent(
      "J. Silva Comércio de Alimentos LtdaPadaria Boa Vista",
    )
    expect(cells[1]).toHaveTextContent("CNPJ 34.218.765/0001-14")
    expect(cells[2]).toHaveTextContent("Araguari/MG, Uberlândia/MG")
    expect(cells[3]).toHaveTextContent("2")
    expect(cells[4]).toHaveTextContent("5")
    expect(cells[5]).toHaveTextContent("Ativo")
    expect(within(silva).getByRole("link", { name: "Abrir" })).toHaveAttribute(
      "href",
      "/clientes/c2",
    )
    expect(row("Ana Paula Ribeiro")).toHaveTextContent("CPF 318.457.260-90")
  })

  it("a série casada aparece na célula do cliente (D1)", async () => {
    serve([
      {
        ...SILVA,
        matchedSerials: [{ serialNumber: "HV9910042", modelName: "SUN2000-5KTL-L1" }],
      },
    ])
    renderScreen({ url: "/clientes?q=HV991" })

    const serial = await screen.findByText("Série HV9910042 · SUN2000-5KTL-L1")

    expect(serial.closest("td")).toBe(
      within(row("J. Silva Comércio de Alimentos Ltda")).getAllByRole("cell")[0],
    )
  })

  it("busca com o placeholder e a dica da web (tela 1)", async () => {
    renderScreen()

    const search = await screen.findByLabelText("Buscar cliente")

    expect(search).toHaveAttribute(
      "placeholder",
      "Nome, nome fantasia, CPF/CNPJ, cidade ou número de série",
    )
    expect(search).toHaveAccessibleDescription(
      "A busca também acha o cliente pelo número de série de um equipamento instalado nele: digite a série da etiqueta.",
    )
  })

  it("contratos de classe: título, guarda da tabela, filtros que quebram, documento sem quebra, alvos de 44 px", async () => {
    renderScreen()
    await screen.findByText("Ana Paula Ribeiro")

    expect(screen.getByRole("heading", { level: 1, name: "Clientes" })).toHaveClass(
      "text-display-lg",
      "max-md:sr-only",
    )
    expect(screen.getByRole("table").parentElement).toHaveClass("overflow-x-auto")

    const search = screen.getByLabelText("Buscar cliente")
    expect(search.parentElement?.parentElement).toHaveClass("md:flex-wrap")

    expect(within(row("Ana Paula Ribeiro")).getAllByRole("cell")[1]).toHaveClass(
      "whitespace-nowrap",
    )
    expect(
      within(row("Ana Paula Ribeiro")).getByRole("link", { name: "Abrir" }),
    ).toHaveClass("min-h-11")
    expect(screen.getByRole("link", { name: "Novo cliente" })).toHaveClass(
      "md:min-h-11",
      "md:text-body-sm",
      "md:font-semibold",
    )
  })

  it('pagina com "Mostrando 1 a 20 de 25 clientes" e Próxima', async () => {
    const user = renderScreen()
    await screen.findByText("Ana Paula Ribeiro")

    expect(screen.getByText("Mostrando 1 a 20 de 25 clientes")).toBeVisible()
    expect(screen.getByRole("button", { name: "Anterior" })).toBeDisabled()

    await user.click(screen.getByRole("button", { name: "Próxima" }))

    expect(await screen.findByText("Mostrando 21 a 25 de 25 clientes")).toBeVisible()
    expect(lastCall()).toEqual({ status: "ACTIVE", page: 1, size: 20 })
  })

  it("trocar a situação volta para a primeira página", async () => {
    const user = renderScreen()
    await screen.findByText("Ana Paula Ribeiro")
    await user.click(screen.getByRole("button", { name: "Próxima" }))
    await screen.findByText("Mostrando 21 a 25 de 25 clientes")

    await user.selectOptions(screen.getByLabelText("Situação"), "Todos")

    await waitFor(() => expect(lastCall()).toEqual({ status: "ALL", page: 0, size: 20 }))
  })
})
