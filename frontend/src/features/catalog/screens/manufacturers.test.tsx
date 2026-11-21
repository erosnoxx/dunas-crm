import { QueryClientProvider } from "@tanstack/react-query"
import { render, screen, waitFor, within } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import { createRoutesStub, Outlet } from "react-router"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"

import { createQueryClient } from "@/shared/lib/query-client"

import * as service from "../catalog.service"
import type { ManufacturerSummary } from "../catalog.service"
import { ManufacturerPanel } from "./manufacturer-panel"
import { ManufacturersScreen } from "./manufacturers"

vi.mock("../catalog.service")

const HUAWEI: ManufacturerSummary = {
  id: "f-huawei",
  name: "Huawei",
  status: "ACTIVE",
  modelCount: 3,
}
const MAKER_B: ManufacturerSummary = {
  id: "f-b",
  name: "Fabricante B",
  status: "ACTIVE",
  modelCount: 1,
}
const MAKER_C: ManufacturerSummary = {
  id: "f-c",
  name: "Fabricante C",
  status: "DEACTIVATED",
  modelCount: 0,
}
const ALL = [HUAWEI, MAKER_B, MAKER_C]

function setDesktop(matches: boolean) {
  window.matchMedia = ((query: string) => ({
    matches,
    media: query,
    addEventListener: () => undefined,
    removeEventListener: () => undefined,
  })) as unknown as typeof window.matchMedia
}

function renderScreen(path = "/catalogo/fabricantes", canEdit = true) {
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
          path: "/catalogo/fabricantes",
          Component: () => <ManufacturersScreen canEdit={canEdit} />,
          children: [
            {
              path: "novo",
              Component: () => <ManufacturerPanel mode="new" canEdit={canEdit} />,
            },
            {
              path: ":id",
              Component: () => <ManufacturerPanel mode="edit" canEdit={canEdit} />,
            },
          ],
        },
      ],
    },
  ])

  render(<Stub initialEntries={[path]} />)

  return userEvent.setup()
}

const originalMatchMedia = window.matchMedia
const row = (name: string) => screen.getByText(name).closest("li") as HTMLElement

beforeEach(() => {
  vi.resetAllMocks()
  vi.mocked(service.listManufacturers).mockResolvedValue({
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

    await screen.findByText("Huawei")

    expect(service.listManufacturers).toHaveBeenCalledWith({
      status: "ALL",
      page: 0,
      size: 100,
    })
  })

  it("cada cartão mostra o número de modelos", async () => {
    renderScreen()
    await screen.findByText("Huawei")

    expect(within(row("Huawei")).getByText("3 modelos")).toBeVisible()
    expect(within(row("Fabricante B")).getByText("1 modelo")).toBeVisible()
    expect(within(row("Fabricante C")).getByText("Nenhum modelo")).toBeVisible()
  })

  it("a situação se lê pela forma: círculo cheio no ativo, vazado no desativado", async () => {
    renderScreen()
    await screen.findByText("Huawei")

    const shape = (name: string, label: string) =>
      within(row(name)).getByText(label).querySelector("svg")

    expect(shape("Huawei", "Ativo")).toHaveAttribute("data-shape", "filled")
    expect(shape("Fabricante C", "Desativado")).toHaveAttribute("data-shape", "ring")
  })

  it("cada cartão é um link para o fabricante", async () => {
    renderScreen()
    await screen.findByText("Huawei")

    expect(within(row("Huawei")).getByRole("link")).toHaveAttribute(
      "href",
      "/catalogo/fabricantes/f-huawei",
    )
  })

  it("o título é só para leitor de tela e a aba Fabricantes é a atual", async () => {
    renderScreen()

    const heading = await screen.findByRole("heading", { level: 1, name: "Catálogo" })

    expect(heading).toHaveClass("text-display-lg", "max-md:sr-only")
    expect(screen.getByRole("link", { name: "Fabricantes" })).toHaveAttribute(
      "aria-current",
      "page",
    )
  })

  it("Novo fabricante é um link fixo acima da barra inferior; sem painel ao lado", async () => {
    renderScreen()

    const link = await screen.findByRole("link", { name: "Novo fabricante" })

    expect(link).toHaveAttribute("href", "/catalogo/fabricantes/novo")
    expect(link.parentElement).toHaveClass("fixed", "bottom-16", "md:hidden")
    expect(screen.getByRole("main")).toHaveClass("max-md:pb-22")
    expect(screen.queryByRole("textbox")).not.toBeInTheDocument()
  })

  it("novo abre a tela cheia com Voltar para a lista, sem a lista nem as abas", async () => {
    renderScreen("/catalogo/fabricantes/novo")

    expect(await screen.findByLabelText("Nome *")).toBeInTheDocument()
    expect(screen.getByRole("link", { name: "Voltar" })).toHaveAttribute(
      "href",
      "/catalogo/fabricantes",
    )
    expect(
      screen.getByRole("heading", { level: 1, name: "Novo fabricante" }),
    ).toBeInTheDocument()
    expect(screen.queryByText("Fabricante B")).not.toBeInTheDocument()
    expect(screen.queryByRole("link", { name: "Fabricantes" })).not.toBeInTheDocument()
  })

  it("Técnico: sem Novo fabricante, e os cartões não são links", async () => {
    renderScreen("/catalogo/fabricantes", false)
    await screen.findByText("Huawei")

    expect(
      screen.queryByRole("link", { name: "Novo fabricante" }),
    ).not.toBeInTheDocument()
    expect(within(row("Huawei")).queryByRole("link")).not.toBeInTheDocument()
    expect(screen.getByRole("main")).not.toHaveClass("max-md:pb-22")
  })

  it("Técnico: a rota novo volta para a lista", async () => {
    renderScreen("/catalogo/fabricantes/novo", false)

    expect(await screen.findByText("3 modelos")).toBeInTheDocument()
    expect(screen.queryByLabelText("Nome *")).not.toBeInTheDocument()
  })

  it("Técnico: a rota do fabricante volta para a lista, sem painel", async () => {
    renderScreen("/catalogo/fabricantes/f-huawei", false)

    expect(await screen.findByText("3 modelos")).toBeInTheDocument()
    expect(screen.queryByRole("textbox")).not.toBeInTheDocument()
    expect(screen.queryByRole("button")).not.toBeInTheDocument()
  })
})

describe("web", () => {
  beforeEach(() => setDesktop(true))

  it("mostra as colunas, o número de modelos e Editar em cada linha", async () => {
    renderScreen()
    await screen.findByText("Huawei")

    for (const column of ["Fabricante", "Situação"])
      expect(screen.getByText(column)).toBeVisible()
    // "Modelos" é também a aba do catálogo.
    expect(screen.getAllByText("Modelos")).toHaveLength(2)
    expect(within(row("Huawei")).getByText("3")).toBeVisible()
    expect(within(row("Huawei")).getByText("Editar")).toBeVisible()
  })

  it("a lista e o painel Novo fabricante ficam lado a lado, sem botão de abrir", async () => {
    renderScreen()

    expect(
      await screen.findByRole("heading", { name: "Novo fabricante" }),
    ).toBeInTheDocument()
    expect(screen.getByLabelText("Nome *")).toBeInTheDocument()
    expect(await screen.findByText("Huawei")).toBeInTheDocument()
    expect(screen.getByRole("button", { name: "Salvar fabricante" })).toBeInTheDocument()
  })

  it("salvar pelo painel da própria lista esvazia o campo, e um segundo envio vazio não chama o serviço", async () => {
    vi.mocked(service.registerManufacturer).mockResolvedValue({ id: "f-new" })
    const user = renderScreen()
    const name = await screen.findByLabelText("Nome *")
    await user.type(name, "Solis")

    await user.click(screen.getByRole("button", { name: "Salvar fabricante" }))

    await waitFor(() =>
      expect(service.registerManufacturer).toHaveBeenCalledWith(
        { name: "Solis" },
        expect.anything(),
      ),
    )
    await waitFor(() => expect(screen.getByLabelText("Nome *")).toHaveValue(""))

    await user.click(screen.getByRole("button", { name: "Salvar fabricante" }))

    expect(await screen.findByText("o nome tem no mínimo 2 caracteres")).toBeVisible()
    expect(service.registerManufacturer).toHaveBeenCalledTimes(1)
  })

  it("abrir um fabricante põe o painel de edição ao lado da lista", async () => {
    const user = renderScreen()

    await screen.findByText("Huawei")
    await user.click(within(row("Huawei")).getByRole("link"))

    expect(await screen.findByLabelText("Nome *")).toHaveValue("Huawei")
    expect(screen.getByText("Fabricante B")).toBeInTheDocument()
    expect(screen.queryByRole("link", { name: "Voltar" })).not.toBeInTheDocument()
  })

  it("Técnico: só a lista, sem painel nem botão", async () => {
    renderScreen("/catalogo/fabricantes", false)
    await screen.findByText("Huawei")

    expect(screen.queryByRole("textbox")).not.toBeInTheDocument()
    expect(screen.queryByRole("button")).not.toBeInTheDocument()
    expect(screen.queryByText("Editar")).not.toBeInTheDocument()
  })
})

describe("estados", () => {
  it("carregando mostra o indicador e nenhuma linha", async () => {
    vi.mocked(service.listManufacturers).mockReturnValue(new Promise(() => undefined))
    renderScreen()

    expect(await screen.findByRole("status")).toHaveAttribute("aria-busy", "true")
    expect(screen.queryAllByRole("listitem")).toHaveLength(0)
  })

  it("erro da consulta sai no aviso, pelo code", async () => {
    vi.mocked(service.listManufacturers).mockRejectedValue({
      status: 403,
      code: "forbidden",
      message: "Forbidden",
    })
    renderScreen()

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Você não tem permissão para isto",
    )
  })

  it("sem fabricantes mostra o vazio", async () => {
    vi.mocked(service.listManufacturers).mockResolvedValue({
      content: [],
      page: 0,
      size: 100,
      totalElements: 0,
      totalPages: 0,
    })
    renderScreen()

    expect(
      await screen.findByRole("heading", { name: "Nenhum fabricante cadastrado" }),
    ).toBeInTheDocument()
  })
})
