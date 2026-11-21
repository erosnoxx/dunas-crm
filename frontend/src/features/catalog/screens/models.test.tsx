import { QueryClientProvider } from "@tanstack/react-query"
import { render, screen, waitFor, within } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import { createRoutesStub, Outlet } from "react-router"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"

import { createQueryClient } from "@/shared/lib/query-client"

import * as service from "../catalog.service"
import type { EquipmentModelSummary, EquipmentTypeSummary } from "../catalog.service"
import { ModelsScreen } from "./models"

vi.mock("../catalog.service")

const INVERTER: EquipmentTypeSummary = {
  id: "t-inverter",
  name: "Inversor",
  status: "ACTIVE",
  modelCount: 3,
  characteristics: [
    { id: "c-power", name: "Potência", unit: "KILOWATT", required: true },
  ],
}

const AIR_CONDITIONER: EquipmentTypeSummary = {
  id: "t-ac",
  name: "Ar-condicionado",
  status: "ACTIVE",
  modelCount: 1,
  characteristics: [
    { id: "c-capacity", name: "Capacidade", unit: "BTU_PER_HOUR", required: true },
  ],
}

const model = (
  n: number,
  over: Partial<EquipmentModelSummary> = {},
): EquipmentModelSummary => ({
  id: `m${n}`,
  name: `Modelo ${n}`,
  status: "ACTIVE",
  manufacturer: { id: "f-huawei", name: "Huawei" },
  type: { id: INVERTER.id, name: INVERTER.name },
  values: [{ definitionId: "c-power", name: "Potência", unit: "KILOWATT", amount: "5" }],
  ...over,
})

const FIRST: EquipmentModelSummary[] = [
  model(1, {
    name: "SUN2000-3KTL-L1",
    values: [{ definitionId: "c-power", name: "Potência", unit: "KILOWATT", amount: 3 }],
  }),
  model(2, {
    name: "SUN2000-8KTL-M1",
    status: "DEACTIVATED",
    values: [
      { definitionId: "c-power", name: "Potência", unit: "KILOWATT", amount: "8.000" },
    ],
  }),
  model(3, {
    name: "Split Inverter 12.000 BTU",
    manufacturer: { id: "f-b", name: "Fabricante B" },
    type: { id: AIR_CONDITIONER.id, name: AIR_CONDITIONER.name },
    values: [
      {
        definitionId: "c-capacity",
        name: "Capacidade",
        unit: "BTU_PER_HOUR",
        amount: "12000",
      },
    ],
  }),
]
const ALL = [...FIRST, ...Array.from({ length: 22 }, (_, i) => model(i + 4))]

function serve(all: EquipmentModelSummary[]) {
  vi.mocked(service.listEquipmentModels).mockImplementation(async ({ page, size }) => ({
    content: all.slice(page * size, (page + 1) * size),
    page,
    size,
    totalElements: all.length,
    totalPages: Math.ceil(all.length / size),
  }))
}

function setDesktop(matches: boolean) {
  window.matchMedia = ((query: string) => ({
    matches,
    media: query,
    addEventListener: () => undefined,
    removeEventListener: () => undefined,
  })) as unknown as typeof window.matchMedia
}

function renderScreen(canEdit = true) {
  const client = createQueryClient(() => undefined)
  const Stub = createRoutesStub([
    {
      Component: () => (
        <QueryClientProvider client={client}>
          <Outlet />
        </QueryClientProvider>
      ),
      children: [
        { path: "/catalogo", Component: () => <ModelsScreen canEdit={canEdit} /> },
      ],
    },
  ])

  render(<Stub initialEntries={["/catalogo"]} />)

  return userEvent.setup()
}

const originalMatchMedia = window.matchMedia
const row = (name: string) => screen.getByText(name).closest("li") as HTMLElement

beforeEach(() => {
  vi.resetAllMocks()
  serve(ALL)
  vi.mocked(service.listEquipmentTypes).mockResolvedValue({
    content: [INVERTER, AIR_CONDITIONER],
    page: 0,
    size: 100,
    totalElements: 2,
    totalPages: 1,
  })
})

afterEach(() => {
  window.matchMedia = originalMatchMedia
})

describe("lista (celular)", () => {
  it("pede os ativos e mostra a característica com o valor e a unidade", async () => {
    renderScreen()

    expect(await screen.findByText("Potência 3 kW")).toBeInTheDocument()
    expect(screen.getByText("Capacidade 12000 BTU/h")).toBeInTheDocument()
    expect(screen.getByText("Potência 8 kW")).toBeInTheDocument()
    expect(service.listEquipmentModels).toHaveBeenCalledWith({
      status: "ACTIVE",
      page: 0,
      size: 20,
    })
  })

  it("cada cartão é um link para o modelo, com o fabricante e o tipo", async () => {
    renderScreen()
    await screen.findByText("SUN2000-3KTL-L1")

    expect(within(row("SUN2000-3KTL-L1")).getByRole("link")).toHaveAttribute(
      "href",
      "/catalogo/modelos/m1",
    )
    expect(
      within(row("Split Inverter 12.000 BTU")).getByText("Fabricante B"),
    ).toBeVisible()
    expect(
      within(row("Split Inverter 12.000 BTU")).getByText("Ar-condicionado"),
    ).toBeVisible()
  })

  it("a situação se lê pela forma: círculo cheio no ativo, vazado no desativado", async () => {
    renderScreen()
    await screen.findByText("SUN2000-3KTL-L1")

    const shape = (name: string, label: string) =>
      within(row(name)).getByText(label).querySelector("svg")

    expect(shape("SUN2000-3KTL-L1", "Ativo")).toHaveAttribute("data-shape", "filled")
    expect(shape("SUN2000-8KTL-M1", "Desativado")).toHaveAttribute("data-shape", "ring")
  })

  it("o título é só para leitor de tela no celular e a aba Modelos é a atual", async () => {
    renderScreen()

    const heading = await screen.findByRole("heading", { level: 1, name: "Catálogo" })

    expect(heading).toHaveClass("text-display-lg", "max-md:sr-only")
    expect(screen.getByRole("link", { name: "Modelos" })).toHaveAttribute(
      "aria-current",
      "page",
    )
  })

  it("Novo modelo é um link para o formulário, fixo acima da barra inferior", async () => {
    renderScreen()

    const link = await screen.findByRole("link", { name: "Novo modelo" })

    expect(link).toHaveAttribute("href", "/catalogo/modelos/novo")
    expect(link.parentElement).toHaveClass("max-md:fixed", "max-md:bottom-16")
    expect(screen.getByRole("main")).toHaveClass("max-md:pb-22")
  })

  it("Técnico: sem Novo modelo e com o aviso de que o papel só consulta", async () => {
    renderScreen(false)
    await screen.findByText("SUN2000-3KTL-L1")

    expect(screen.queryByRole("link", { name: "Novo modelo" })).not.toBeInTheDocument()
    expect(
      screen.getByText(/Seu papel \(Técnico\) consulta o catálogo/),
    ).toBeInTheDocument()
    expect(screen.getByRole("main")).not.toHaveClass("max-md:pb-22")
  })

  it('mostra "Mostrando 20 de 25 modelos" e Carregar mais acrescenta a próxima página', async () => {
    const user = renderScreen()
    await screen.findByText("SUN2000-3KTL-L1")

    expect(screen.getByText("Mostrando 20 de 25 modelos")).toBeVisible()

    await user.click(screen.getByRole("button", { name: "Carregar mais" }))

    expect(await screen.findByText("Modelo 25")).toBeInTheDocument()
    expect(service.listEquipmentModels).toHaveBeenLastCalledWith({
      status: "ACTIVE",
      page: 1,
      size: 20,
    })
    expect(screen.getByText("SUN2000-3KTL-L1")).toBeInTheDocument()
    expect(screen.getByText("Mostrando 25 de 25 modelos")).toBeVisible()
  })
})

describe("filtros", () => {
  it("o tipo vem dos tipos cadastrados e filtra pelo id", async () => {
    const user = renderScreen()
    await screen.findByText("SUN2000-3KTL-L1")

    await user.selectOptions(screen.getByLabelText("Tipo"), "Ar-condicionado")

    await waitFor(() =>
      expect(service.listEquipmentModels).toHaveBeenLastCalledWith({
        typeId: "t-ac",
        status: "ACTIVE",
        page: 0,
        size: 20,
      }),
    )
    expect(service.listEquipmentTypes).toHaveBeenCalledWith({
      status: "ALL",
      page: 0,
      size: 100,
    })
  })

  it.each([
    ["Desativados", "DEACTIVATED"],
    ["Todos", "ALL"],
  ] as const)("situação %s pede %s", async (label, status) => {
    const user = renderScreen()
    await screen.findByText("SUN2000-3KTL-L1")

    await user.selectOptions(screen.getByLabelText("Situação"), label)

    await waitFor(() =>
      expect(service.listEquipmentModels).toHaveBeenLastCalledWith({
        status,
        page: 0,
        size: 20,
      }),
    )
  })

  it("a busca vai como texto", async () => {
    const user = renderScreen()
    await screen.findByText("SUN2000-3KTL-L1")

    await user.type(screen.getByLabelText("Buscar modelo"), "SUN")

    await waitFor(() =>
      expect(service.listEquipmentModels).toHaveBeenLastCalledWith({
        text: "SUN",
        status: "ACTIVE",
        page: 0,
        size: 20,
      }),
    )
  })

  it("busca sem resultado mostra o vazio, e Limpar busca e filtros refaz a consulta", async () => {
    const user = renderScreen()
    await screen.findByText("SUN2000-3KTL-L1")
    vi.mocked(service.listEquipmentModels).mockImplementation(async (input) =>
      input.text
        ? { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 }
        : {
            content: FIRST,
            page: 0,
            size: 20,
            totalElements: FIRST.length,
            totalPages: 1,
          },
    )

    await user.type(screen.getByLabelText("Buscar modelo"), "XPTO-9000")

    expect(
      await screen.findByRole("heading", { name: "Nenhum modelo encontrado" }),
    ).toBeInTheDocument()
    expect(
      screen.getByText(
        'Nada no catálogo bate com "XPTO-9000" entre os modelos ativos. Confira a busca ou cadastre o modelo.',
      ),
    ).toBeInTheDocument()

    await user.click(screen.getByRole("button", { name: "Limpar busca e filtros" }))

    expect(await screen.findByText("SUN2000-3KTL-L1")).toBeInTheDocument()
    expect(screen.getByLabelText("Buscar modelo")).toHaveValue("")
    expect(service.listEquipmentModels).toHaveBeenLastCalledWith({
      status: "ACTIVE",
      page: 0,
      size: 20,
    })
  })
})

describe("web", () => {
  beforeEach(() => setDesktop(true))

  it("mostra as colunas da tabela e Abrir em cada linha", async () => {
    renderScreen()
    await screen.findByText("SUN2000-3KTL-L1")

    for (const column of ["Fabricante", "Modelo", "Característica"])
      expect(screen.getByText(column)).toBeVisible()
    expect(within(row("SUN2000-3KTL-L1")).getByText("Abrir")).toBeVisible()
  })

  it('pagina com "Mostrando 1 a 20 de 25 modelos" e Próxima', async () => {
    const user = renderScreen()
    await screen.findByText("SUN2000-3KTL-L1")

    expect(screen.getByText("Mostrando 1 a 20 de 25 modelos")).toBeVisible()
    expect(screen.getByRole("button", { name: "Anterior" })).toBeDisabled()

    await user.click(screen.getByRole("button", { name: "Próxima" }))

    expect(await screen.findByText("Mostrando 21 a 25 de 25 modelos")).toBeVisible()
    expect(service.listEquipmentModels).toHaveBeenLastCalledWith({
      status: "ACTIVE",
      page: 1,
      size: 20,
    })
  })

  it("trocar o filtro volta para a primeira página", async () => {
    const user = renderScreen()
    await screen.findByText("SUN2000-3KTL-L1")
    await user.click(screen.getByRole("button", { name: "Próxima" }))
    await screen.findByText("Mostrando 21 a 25 de 25 modelos")

    await user.selectOptions(screen.getByLabelText("Situação"), "Todos")

    await waitFor(() =>
      expect(service.listEquipmentModels).toHaveBeenLastCalledWith({
        status: "ALL",
        page: 0,
        size: 20,
      }),
    )
  })
})

describe("estados", () => {
  it("carregando mostra o indicador e nenhuma linha", async () => {
    vi.mocked(service.listEquipmentModels).mockReturnValue(new Promise(() => undefined))
    renderScreen()

    expect(await screen.findByRole("status")).toHaveAttribute("aria-busy", "true")
    expect(screen.queryAllByRole("listitem")).toHaveLength(0)
  })

  it("erro da consulta sai no aviso, pelo code", async () => {
    vi.mocked(service.listEquipmentModels).mockRejectedValue({
      status: 403,
      code: "forbidden",
      message: "Forbidden",
    })
    renderScreen()

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Você não tem permissão para isto",
    )
    expect(screen.queryAllByRole("listitem")).toHaveLength(0)
  })

  it("catálogo vazio, sem filtro, não oferece limpar", async () => {
    serve([])
    renderScreen()

    expect(
      await screen.findByRole("heading", { name: "Nenhum modelo encontrado" }),
    ).toBeInTheDocument()
    expect(
      screen.queryByRole("button", { name: "Limpar busca e filtros" }),
    ).not.toBeInTheDocument()
  })
})
