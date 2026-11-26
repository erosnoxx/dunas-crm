import { QueryClientProvider } from "@tanstack/react-query"
import { render, screen, waitFor, within } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import { createRoutesStub, Outlet } from "react-router"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"

import { createQueryClient } from "@/shared/lib/query-client"

import * as service from "../catalog.service"
import type { EquipmentTypeSummary } from "../catalog.service"
import { TypePanel } from "./type-panel"
import { TypesScreen } from "./types"

vi.mock("../catalog.service")

const INVERSOR: EquipmentTypeSummary = {
  id: "t-inv",
  name: "Inversor",
  status: "ACTIVE",
  modelCount: 3,
  characteristics: [{ id: "c-pot", name: "Potência", unit: "KILOWATT", required: true }],
}
const AR: EquipmentTypeSummary = {
  id: "t-ar",
  name: "Ar-condicionado",
  status: "ACTIVE",
  modelCount: 1,
  characteristics: [
    { id: "c-cap", name: "Capacidade", unit: "BTU_PER_HOUR", required: true },
  ],
}
const BATERIA: EquipmentTypeSummary = {
  id: "t-bat",
  name: "Bateria",
  status: "DEACTIVATED",
  modelCount: 0,
  characteristics: [{ id: "c-ene", name: "Energia", unit: "WATT", required: false }],
}
const ALL = [INVERSOR, AR, BATERIA]

function setDesktop(matches: boolean) {
  window.matchMedia = ((query: string) => ({
    matches,
    media: query,
    addEventListener: () => undefined,
    removeEventListener: () => undefined,
  })) as unknown as typeof window.matchMedia
}

function renderScreen(path = "/catalogo/tipos", canEdit = true) {
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
          path: "/catalogo/tipos",
          Component: () => <TypesScreen canEdit={canEdit} />,
          children: [
            { path: "novo", Component: () => <TypePanel mode="new" canEdit={canEdit} /> },
            { path: ":id", Component: () => <TypePanel mode="edit" canEdit={canEdit} /> },
          ],
        },
      ],
    },
  ])

  render(<Stub initialEntries={[path]} />)

  return userEvent.setup()
}

const originalMatchMedia = window.matchMedia
const row = (name: string) => screen.getByText(name).closest("li, tr") as HTMLElement

beforeEach(() => {
  vi.resetAllMocks()
  vi.mocked(service.listEquipmentTypes).mockResolvedValue({
    content: ALL,
    page: 0,
    size: 100,
    totalElements: ALL.length,
    totalPages: 1,
  })
})

afterEach(() => {
  window.matchMedia = originalMatchMedia
})

describe("lista (celular)", () => {
  it("pede todas as situações, para o desativado continuar ao alcance de reativar", async () => {
    renderScreen()

    await screen.findByText("Inversor")

    expect(service.listEquipmentTypes).toHaveBeenCalledWith({
      status: "ALL",
      page: 0,
      size: 100,
    })
  })

  it("cada cartão mostra a característica com a unidade e o número de modelos", async () => {
    renderScreen()
    await screen.findByText("Inversor")

    expect(
      within(row("Inversor")).getByText("Característica: Potência (kW)"),
    ).toBeVisible()
    expect(within(row("Inversor")).getByText("3 modelos")).toBeVisible()
    expect(
      within(row("Ar-condicionado")).getByText("Característica: Capacidade (BTU/h)"),
    ).toBeVisible()
    expect(within(row("Ar-condicionado")).getByText("1 modelo")).toBeVisible()
    expect(within(row("Bateria")).getByText("Nenhum modelo")).toBeVisible()
  })

  it("a situação se lê pela forma: círculo cheio no ativo, vazado no desativado", async () => {
    renderScreen()
    await screen.findByText("Inversor")

    const shape = (name: string, label: string) =>
      within(row(name)).getByText(label).querySelector("svg")

    expect(shape("Inversor", "Ativo")).toHaveAttribute("data-shape", "filled")
    expect(shape("Bateria", "Desativado")).toHaveAttribute("data-shape", "ring")
  })

  it("cada cartão é um link para o tipo", async () => {
    renderScreen()
    await screen.findByText("Inversor")

    expect(within(row("Inversor")).getByRole("link")).toHaveAttribute(
      "href",
      "/catalogo/tipos/t-inv",
    )
  })

  it("o título é só para leitor de tela e a aba Tipos é a atual", async () => {
    renderScreen()

    const heading = await screen.findByRole("heading", { level: 1, name: "Catálogo" })

    expect(heading).toHaveClass("text-display-lg", "max-md:sr-only")
    expect(screen.getByRole("link", { name: "Tipos" })).toHaveAttribute(
      "aria-current",
      "page",
    )
  })

  it("Novo tipo é um link fixo acima da barra inferior; sem painel ao lado", async () => {
    renderScreen()

    const link = await screen.findByRole("link", { name: "Novo tipo" })

    expect(link).toHaveAttribute("href", "/catalogo/tipos/novo")
    expect(link.parentElement).toHaveClass("fixed", "bottom-16", "md:hidden")
    expect(screen.getByRole("main")).toHaveClass("max-md:pb-22")
    expect(screen.queryByRole("textbox")).not.toBeInTheDocument()
  })

  it("novo abre a tela cheia com Voltar para a lista, sem a lista nem as abas", async () => {
    renderScreen("/catalogo/tipos/novo")

    expect(await screen.findByLabelText("Nome do tipo *")).toBeInTheDocument()
    expect(screen.getByRole("link", { name: "Voltar" })).toHaveAttribute(
      "href",
      "/catalogo/tipos",
    )
    expect(
      screen.getByRole("heading", { level: 1, name: "Novo tipo" }),
    ).toBeInTheDocument()
    expect(screen.queryByText("Ar-condicionado")).not.toBeInTheDocument()
    expect(screen.queryByRole("link", { name: "Tipos" })).not.toBeInTheDocument()
  })

  it("abrir um tipo é a tela cheia de edição, com os dados e Voltar para a lista", async () => {
    const user = renderScreen()

    await screen.findByText("Inversor")
    await user.click(within(row("Inversor")).getByRole("link"))

    expect(await screen.findByLabelText("Nome do tipo *")).toHaveValue("Inversor")
    expect(
      screen.getByRole("heading", { level: 1, name: "Editar tipo" }),
    ).toBeInTheDocument()
    expect(screen.getByRole("link", { name: "Voltar" })).toHaveAttribute(
      "href",
      "/catalogo/tipos",
    )
  })

  it("Técnico: sem Novo tipo, e os cartões não são links", async () => {
    renderScreen("/catalogo/tipos", false)
    await screen.findByText("Inversor")

    expect(screen.queryByRole("link", { name: "Novo tipo" })).not.toBeInTheDocument()
    expect(within(row("Inversor")).queryByRole("link")).not.toBeInTheDocument()
    expect(screen.getByRole("main")).not.toHaveClass("max-md:pb-22")
  })

  it("Técnico: a rota novo volta para a lista, sem painel", async () => {
    renderScreen("/catalogo/tipos/novo", false)

    expect(await screen.findByText("3 modelos")).toBeInTheDocument()
    expect(screen.queryByLabelText("Nome do tipo *")).not.toBeInTheDocument()
  })

  it("Técnico: a rota do tipo volta para a lista, sem painel nem botão", async () => {
    renderScreen("/catalogo/tipos/t-inv", false)

    expect(await screen.findByText("3 modelos")).toBeInTheDocument()
    expect(screen.queryByRole("textbox")).not.toBeInTheDocument()
    expect(screen.queryByRole("button")).not.toBeInTheDocument()
  })
})

describe("web", () => {
  beforeEach(() => setDesktop(true))

  it("mostra as colunas e, em cada linha, a característica, a unidade, o número de modelos e Editar", async () => {
    renderScreen()
    await screen.findByText("Inversor")

    const table = screen.getByRole("table")
    for (const column of ["Tipo", "Característica", "Unidade", "Modelos", "Situação"])
      expect(within(table).getByRole("columnheader", { name: column })).toBeVisible()
    expect(within(row("Inversor")).getByText("Potência")).toBeVisible()
    expect(within(row("Inversor")).getByText("kW")).toBeVisible()
    expect(within(row("Inversor")).getByText("3")).toBeVisible()
    expect(within(row("Ar-condicionado")).getByText("BTU/h")).toBeVisible()
    expect(within(row("Inversor")).getByRole("link", { name: "Editar" })).toHaveAttribute(
      "href",
      "/catalogo/tipos/t-inv",
    )
  })

  it("Editar tem alvo de 44 px de altura, dentro da linha de 56", async () => {
    renderScreen()
    await screen.findByText("Inversor")

    expect(within(row("Inversor")).getByRole("link", { name: "Editar" })).toHaveClass(
      "min-h-11",
    )
  })

  it("a lista e o painel Novo tipo ficam lado a lado, sem botão de abrir", async () => {
    renderScreen()

    expect(await screen.findByRole("heading", { name: "Novo tipo" })).toBeInTheDocument()
    expect(screen.getByLabelText("Nome do tipo *")).toBeInTheDocument()
    expect(await screen.findByText("Inversor")).toBeInTheDocument()
    expect(screen.getByRole("button", { name: "Salvar tipo" })).toBeInTheDocument()
  })

  it("salvar pelo painel da própria lista esvazia o formulário, e um segundo envio vazio não chama o serviço", async () => {
    vi.mocked(service.registerEquipmentType).mockResolvedValue({ id: "t-new" })
    const user = renderScreen()
    await user.type(await screen.findByLabelText("Nome do tipo *"), "Painel solar")
    await user.type(screen.getByLabelText("Nome da característica *"), "Potência")
    await user.selectOptions(screen.getByLabelText("Unidade *"), "Wp")

    await user.click(screen.getByRole("button", { name: "Salvar tipo" }))

    await waitFor(() => expect(service.registerEquipmentType).toHaveBeenCalledTimes(1))
    await waitFor(() => expect(screen.getByLabelText("Nome do tipo *")).toHaveValue(""))
    expect(screen.getByLabelText("Nome da característica *")).toHaveValue("")
    expect(screen.getByLabelText("Unidade *")).toHaveValue("")

    await user.click(screen.getByRole("button", { name: "Salvar tipo" }))

    expect(await screen.findByText("o nome tem no mínimo 2 caracteres")).toBeVisible()
    expect(service.registerEquipmentType).toHaveBeenCalledTimes(1)
  })

  it("abrir um tipo põe o painel de edição ao lado da lista e marca a linha como a aberta", async () => {
    const user = renderScreen()

    await screen.findByText("Inversor")
    await user.click(within(row("Inversor")).getByRole("link", { name: "Editar" }))

    expect(await screen.findByLabelText("Nome do tipo *")).toHaveValue("Inversor")
    expect(screen.getByText("Ar-condicionado")).toBeInTheDocument()
    expect(screen.queryByRole("link", { name: "Voltar" })).not.toBeInTheDocument()
    expect(
      within(row("Inversor")).getByRole("link", { name: "Editando" }),
    ).toHaveAttribute("aria-current", "true")
  })

  it("Técnico: só a lista, sem painel, botão nem Editar", async () => {
    renderScreen("/catalogo/tipos", false)
    await screen.findByText("Inversor")

    expect(screen.queryByRole("textbox")).not.toBeInTheDocument()
    expect(screen.queryByRole("button")).not.toBeInTheDocument()
    expect(screen.queryByText("Editar")).not.toBeInTheDocument()
    expect(screen.getByText("Potência")).toBeVisible()
  })
})

describe("estados", () => {
  it("carregando mostra o indicador e nenhuma linha", async () => {
    vi.mocked(service.listEquipmentTypes).mockReturnValue(new Promise(() => undefined))
    renderScreen()

    expect(await screen.findByRole("status")).toHaveAttribute("aria-busy", "true")
    expect(screen.queryAllByRole("listitem")).toHaveLength(0)
  })

  it("erro da consulta sai no aviso, pelo code", async () => {
    vi.mocked(service.listEquipmentTypes).mockRejectedValue({
      status: 403,
      code: "forbidden",
      message: "Forbidden",
    })
    renderScreen()

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Você não tem permissão para isto",
    )
  })

  it("sem tipos mostra o vazio", async () => {
    vi.mocked(service.listEquipmentTypes).mockResolvedValue({
      content: [],
      page: 0,
      size: 100,
      totalElements: 0,
      totalPages: 0,
    })
    renderScreen()

    expect(
      await screen.findByRole("heading", { name: "Nenhum tipo cadastrado" }),
    ).toBeInTheDocument()
  })
})
