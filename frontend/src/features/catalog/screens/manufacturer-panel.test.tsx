import { QueryClientProvider } from "@tanstack/react-query"
import { render, screen, waitFor } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import { createRoutesStub, Outlet } from "react-router"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"

import { createQueryClient } from "@/shared/lib/query-client"

import * as service from "../catalog.service"
import type { ManufacturerSummary } from "../catalog.service"
import { manufacturerSchema } from "../catalog.schemas"
import { ManufacturerPanel } from "./manufacturer-panel"

vi.mock("../catalog.service")

const HUAWEI: ManufacturerSummary = {
  id: "f-huawei",
  name: "Huawei",
  status: "ACTIVE",
  modelCount: 3,
}

const page = (content: ManufacturerSummary[]) => ({
  content,
  page: 0,
  size: 100,
  totalElements: content.length,
  totalPages: 1,
})

function setDesktop(matches: boolean) {
  window.matchMedia = ((query: string) => ({
    matches,
    media: query,
    addEventListener: () => undefined,
    removeEventListener: () => undefined,
  })) as unknown as typeof window.matchMedia
}

function renderPanel(path: string, canEdit = true) {
  const client = createQueryClient(() => undefined)
  const Stub = createRoutesStub([
    {
      Component: () => (
        <QueryClientProvider client={client}>
          <Outlet />
        </QueryClientProvider>
      ),
      children: [
        { path: "/catalogo/fabricantes", Component: () => <p>lista de fabricantes</p> },
        {
          path: "/catalogo/fabricantes/novo",
          Component: () => <ManufacturerPanel mode="new" canEdit={canEdit} />,
        },
        {
          path: "/catalogo/fabricantes/:id",
          Component: () => <ManufacturerPanel mode="edit" canEdit={canEdit} />,
        },
      ],
    },
  ])

  render(<Stub initialEntries={[path]} />)

  return userEvent.setup()
}

const originalMatchMedia = window.matchMedia
const button = (name: string) => screen.getByRole("button", { name })

beforeEach(() => {
  vi.resetAllMocks()
  vi.mocked(service.listManufacturers).mockResolvedValue(page([HUAWEI]))
})

afterEach(() => {
  window.matchMedia = originalMatchMedia
})

describe("Novo fabricante", () => {
  it("Salvar fabricante envia o nome e volta para a lista", async () => {
    vi.mocked(service.registerManufacturer).mockResolvedValue({ id: "f-new" })
    const user = renderPanel("/catalogo/fabricantes/novo")

    await user.type(await screen.findByLabelText("Nome *"), "  Solis  ")
    await user.click(button("Salvar fabricante"))

    await waitFor(() =>
      expect(service.registerManufacturer).toHaveBeenCalledWith(
        { name: "Solis" },
        expect.anything(),
      ),
    )
    expect(await screen.findByText("lista de fabricantes")).toBeInTheDocument()
  })

  it("manufacturer_name_taken sai sob o nome, com ícone e texto, e o texto digitado fica", async () => {
    vi.mocked(service.registerManufacturer).mockRejectedValue({
      status: 409,
      code: "manufacturer_name_taken",
      message: "x",
    })
    const user = renderPanel("/catalogo/fabricantes/novo")
    const name = await screen.findByLabelText("Nome *")
    await user.type(name, "Huawei")

    await user.click(button("Salvar fabricante"))

    const error = await screen.findByText("já existe um fabricante com este nome")
    expect(error.querySelector("svg")).toBeInTheDocument()
    expect(name).toHaveValue("Huawei")
    expect(name).toHaveAttribute("aria-invalid", "true")
    expect(name).toHaveAccessibleDescription("já existe um fabricante com este nome")
    expect(screen.queryByRole("alert")).not.toBeInTheDocument()
  })

  it("um caractere é recusado pelo mínimo de 2, sem chamar o serviço", async () => {
    const user = renderPanel("/catalogo/fabricantes/novo")

    await user.type(await screen.findByLabelText("Nome *"), "H")
    await user.click(button("Salvar fabricante"))

    expect(await screen.findByText("o nome tem no mínimo 2 caracteres")).toBeVisible()
    expect(service.registerManufacturer).not.toHaveBeenCalled()
  })

  it("o campo aceita no máximo 80 caracteres, que é o limite do schema", async () => {
    const user = renderPanel("/catalogo/fabricantes/novo")
    const name = await screen.findByLabelText("Nome *")

    await user.type(name, "a".repeat(85))

    expect(name).toHaveAttribute("maxlength", "80")
    expect(name).toHaveValue("a".repeat(80))
    expect(manufacturerSchema.safeParse({ name: "a".repeat(80) }).success).toBe(true)
    expect(manufacturerSchema.safeParse({ name: "a".repeat(81) }).success).toBe(false)
    expect(manufacturerSchema.safeParse({ name: "a" }).success).toBe(false)
  })

  it("concurrent_update sai no aviso, sem perder o que foi digitado", async () => {
    vi.mocked(service.registerManufacturer).mockRejectedValue({
      status: 409,
      code: "concurrent_update",
      message: "x",
    })
    const user = renderPanel("/catalogo/fabricantes/novo")
    const name = await screen.findByLabelText("Nome *")
    await user.type(name, "Solis")

    await user.click(button("Salvar fabricante"))

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "O registro foi alterado ao mesmo tempo por outra operação; tente de novo",
    )
    expect(name).toHaveValue("Solis")
  })

  it("no celular o título é o do cabeçalho e Salvar fica fixo no rodapé", async () => {
    renderPanel("/catalogo/fabricantes/novo")

    expect(
      await screen.findByRole("heading", { level: 1, name: "Novo fabricante" }),
    ).toBeInTheDocument()
    expect(button("Salvar fabricante").parentElement).toHaveClass("fixed", "bottom-0")
  })

  it("Técnico: a rota volta para a lista", async () => {
    renderPanel("/catalogo/fabricantes/novo", false)

    expect(await screen.findByText("lista de fabricantes")).toBeInTheDocument()
    expect(screen.queryByRole("textbox")).not.toBeInTheDocument()
  })
})

describe("Editar fabricante", () => {
  it("vem com o nome preenchido, e Salvar alterações renomeia e volta para a lista", async () => {
    vi.mocked(service.renameManufacturer).mockResolvedValue()
    const user = renderPanel("/catalogo/fabricantes/f-huawei")
    const name = await screen.findByLabelText("Nome *")
    expect(name).toHaveValue("Huawei")

    await user.clear(name)
    await user.type(name, "Huawei Solar")
    await user.click(button("Salvar alterações"))

    await waitFor(() =>
      expect(service.renameManufacturer).toHaveBeenCalledWith("f-huawei", {
        name: "Huawei Solar",
      }),
    )
    expect(await screen.findByText("lista de fabricantes")).toBeInTheDocument()
  })

  it("manufacturer_name_taken ao renomear mantém o texto digitado", async () => {
    vi.mocked(service.renameManufacturer).mockRejectedValue({
      status: 409,
      code: "manufacturer_name_taken",
      message: "x",
    })
    const user = renderPanel("/catalogo/fabricantes/f-huawei")
    const name = await screen.findByLabelText("Nome *")
    await user.clear(name)
    await user.type(name, "Fabricante B")

    await user.click(button("Salvar alterações"))

    expect(await screen.findByText("já existe um fabricante com este nome")).toBeVisible()
    expect(name).toHaveValue("Fabricante B")
  })

  it("manufacturer_deactivated sai no aviso e a consulta é refeita", async () => {
    vi.mocked(service.renameManufacturer).mockRejectedValue({
      status: 409,
      code: "manufacturer_deactivated",
      message: "x",
    })
    const user = renderPanel("/catalogo/fabricantes/f-huawei")
    await screen.findByLabelText("Nome *")
    vi.mocked(service.listManufacturers).mockResolvedValue(
      page([{ ...HUAWEI, status: "DEACTIVATED" }]),
    )

    await user.click(button("Salvar alterações"))

    expect(
      await screen.findByRole("button", { name: "Reativar fabricante" }),
    ).toBeVisible()
  })

  it("Desativar fabricante chama o serviço, sem confirmação, e a tela passa a Reativar fabricante", async () => {
    vi.mocked(service.deactivateManufacturer).mockImplementation(async () => {
      vi.mocked(service.listManufacturers).mockResolvedValue(
        page([{ ...HUAWEI, status: "DEACTIVATED" }]),
      )
    })
    const user = renderPanel("/catalogo/fabricantes/f-huawei")

    await user.click(await screen.findByRole("button", { name: "Desativar fabricante" }))

    expect(service.deactivateManufacturer).toHaveBeenCalledWith(
      "f-huawei",
      expect.anything(),
    )
    expect(
      await screen.findByRole("button", { name: "Reativar fabricante" }),
    ).toBeInTheDocument()
    expect(
      screen.queryByRole("button", { name: "Desativar fabricante" }),
    ).not.toBeInTheDocument()
  })

  it("desativado: nome só leitura, a situação por forma e só Reativar fabricante", async () => {
    vi.mocked(service.listManufacturers).mockResolvedValue(
      page([{ ...HUAWEI, status: "DEACTIVATED" }]),
    )
    vi.mocked(service.reactivateManufacturer).mockImplementation(async () => {
      vi.mocked(service.listManufacturers).mockResolvedValue(page([HUAWEI]))
    })
    const user = renderPanel("/catalogo/fabricantes/f-huawei")

    expect(await screen.findByLabelText("Nome *")).toBeDisabled()
    expect(screen.getByLabelText("Nome *")).toHaveValue("Huawei")
    expect(screen.getByText("Desativado").querySelector("svg")).toHaveAttribute(
      "data-shape",
      "ring",
    )
    expect(screen.queryByRole("button", { name: "Salvar alterações" })).toBeNull()
    expect(screen.queryByRole("button", { name: "Desativar fabricante" })).toBeNull()

    await user.click(button("Reativar fabricante"))

    expect(service.reactivateManufacturer).toHaveBeenCalledWith(
      "f-huawei",
      expect.anything(),
    )
    expect(
      await screen.findByRole("button", { name: "Desativar fabricante" }),
    ).toBeInTheDocument()
    expect(screen.getByLabelText("Nome *")).toBeEnabled()
  })

  it("erro ao desativar sai no aviso, pelo code", async () => {
    vi.mocked(service.deactivateManufacturer).mockRejectedValue({
      status: 404,
      code: "manufacturer_not_found",
      message: "x",
    })
    const user = renderPanel("/catalogo/fabricantes/f-huawei")

    await user.click(await screen.findByRole("button", { name: "Desativar fabricante" }))

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Fabricante não encontrado",
    )
  })

  it("id que a lista não tem: manufacturer_not_found no aviso", async () => {
    renderPanel("/catalogo/fabricantes/outro")

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Fabricante não encontrado",
    )
  })

  it("Técnico: a rota volta para a lista, sem painel nem botão", async () => {
    renderPanel("/catalogo/fabricantes/f-huawei", false)

    expect(await screen.findByText("lista de fabricantes")).toBeInTheDocument()
    expect(screen.queryByRole("button")).not.toBeInTheDocument()
  })
})

describe("web", () => {
  beforeEach(() => setDesktop(true))

  it("o título é o h2 do painel e Salvar fica dentro dele, sem rodapé fixo", async () => {
    renderPanel("/catalogo/fabricantes/novo")

    expect(
      await screen.findByRole("heading", { level: 2, name: "Novo fabricante" }),
    ).toBeInTheDocument()
    expect(screen.queryByRole("link", { name: "Voltar" })).not.toBeInTheDocument()
    expect(button("Salvar fabricante").parentElement).not.toHaveClass("fixed")
  })

  it("a edição tem Cancelar, que leva para a lista", async () => {
    const user = renderPanel("/catalogo/fabricantes/f-huawei")

    await user.click(await screen.findByRole("link", { name: "Cancelar" }))

    expect(await screen.findByText("lista de fabricantes")).toBeInTheDocument()
  })
})
