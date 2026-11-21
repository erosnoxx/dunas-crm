import { QueryClientProvider } from "@tanstack/react-query"
import { act, render, screen, waitFor } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import { createRoutesStub, Outlet } from "react-router"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"

import { createQueryClient } from "@/shared/lib/query-client"

import * as service from "../catalog.service"
import type {
  EquipmentModelSummary,
  EquipmentTypeSummary,
  ManufacturerSummary,
} from "../catalog.service"
import { ModelFormScreen } from "./model-form"

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

const SUN: EquipmentModelSummary = {
  id: "m1",
  name: "SUN2000-3KTL-L1",
  status: "ACTIVE",
  manufacturer: { id: HUAWEI.id, name: HUAWEI.name },
  type: { id: INVERTER.id, name: INVERTER.name },
  values: [{ definitionId: "c-power", name: "Potência", unit: "KILOWATT", amount: "3" }],
}

const page = <T,>(content: T[]) => ({
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

function renderForm(path: string, canEdit = true) {
  const client = createQueryClient(() => undefined)
  const Stub = createRoutesStub([
    {
      Component: () => (
        <QueryClientProvider client={client}>
          <Outlet />
        </QueryClientProvider>
      ),
      children: [
        { path: "/catalogo", Component: () => <p>lista de modelos</p> },
        {
          path: "/catalogo/modelos/novo",
          Component: () => <ModelFormScreen mode="new" canEdit={canEdit} />,
        },
        {
          path: "/catalogo/modelos/:id",
          Component: () => <ModelFormScreen mode="edit" canEdit={canEdit} />,
        },
      ],
    },
  ])

  render(<Stub initialEntries={[path]} />)

  return { user: userEvent.setup(), client }
}

const originalMatchMedia = window.matchMedia
const save = (name = "Salvar modelo") => screen.getByRole("button", { name })

async function fillNew(user: ReturnType<typeof userEvent.setup>, amount: string) {
  await screen.findByRole("option", { name: "Fabricante B" })
  await screen.findByRole("option", { name: "Ar-condicionado" })
  await user.selectOptions(screen.getByLabelText("Fabricante *"), "Fabricante B")
  await user.selectOptions(screen.getByLabelText("Tipo *"), "Ar-condicionado")
  await user.type(screen.getByLabelText("Nome do modelo *"), "Split Inverter 12.000 BTU")
  await user.type(screen.getByLabelText("Capacidade *"), amount)
}

beforeEach(() => {
  vi.resetAllMocks()
  vi.mocked(service.listManufacturers).mockResolvedValue(page([HUAWEI, MAKER_B]))
  vi.mocked(service.listEquipmentTypes).mockResolvedValue(
    page([INVERTER, AIR_CONDITIONER]),
  )
  vi.mocked(service.getEquipmentModel).mockResolvedValue(SUN)
})

afterEach(() => {
  window.matchMedia = originalMatchMedia
})

describe("novo modelo", () => {
  it("fabricante e tipo são escolhas só entre os ativos", async () => {
    renderForm("/catalogo/modelos/novo")

    expect(
      await screen.findByRole("option", { name: "Fabricante B" }),
    ).toBeInTheDocument()
    expect(service.listManufacturers).toHaveBeenCalledWith({
      status: "ACTIVE",
      page: 0,
      size: 100,
    })
    expect(service.listEquipmentTypes).toHaveBeenCalledWith({
      status: "ACTIVE",
      page: 0,
      size: 100,
    })
  })

  it.each([
    ["fabricantes", () => vi.mocked(service.listManufacturers)],
    ["tipos", () => vi.mocked(service.listEquipmentTypes)],
  ])(
    "a consulta de %s com falha mostra o aviso, não culpa a pessoa",
    async (_, query) => {
      query().mockRejectedValue({ status: 403, code: "forbidden", message: "Forbidden" })
      renderForm("/catalogo/modelos/novo")

      expect(await screen.findByRole("alert")).toHaveTextContent(
        "Você não tem permissão para isto",
      )
    },
  )

  it("enquanto as escolhas carregam mostra o indicador de carregamento", async () => {
    vi.mocked(service.listManufacturers).mockReturnValue(new Promise(() => undefined))
    renderForm("/catalogo/modelos/novo")

    expect(await screen.findByRole("status")).toHaveAttribute("aria-busy", "true")
  })

  it("trocar o tipo troca o rótulo e a unidade, e zera o valor", async () => {
    const { user } = renderForm("/catalogo/modelos/novo")
    await screen.findByRole("option", { name: "Inversor" })

    await user.selectOptions(screen.getByLabelText("Tipo *"), "Inversor")
    await user.type(screen.getByLabelText("Potência *"), "3")
    expect(screen.getByText("kW")).toBeInTheDocument()

    await user.selectOptions(screen.getByLabelText("Tipo *"), "Ar-condicionado")

    expect(screen.queryByLabelText("Potência *")).not.toBeInTheDocument()
    expect(screen.getByLabelText("Capacidade *")).toHaveValue("")
    expect(screen.getByText("BTU/h")).toBeInTheDocument()
  })

  it('"12.000" mostra "use só números e vírgula" e não envia', async () => {
    const { user } = renderForm("/catalogo/modelos/novo")
    await fillNew(user, "12.000")

    await user.click(save())

    expect(await screen.findByText(/use só números e vírgula/)).toBeInTheDocument()
    expect(screen.getByLabelText("Capacidade *")).toHaveAttribute("aria-invalid", "true")
    expect(screen.getByLabelText("Capacidade *")).toHaveFocus()
    expect(service.registerEquipmentModel).not.toHaveBeenCalled()
  })

  it('"12000" envia amount "12000" e volta para a lista', async () => {
    vi.mocked(service.registerEquipmentModel).mockResolvedValue({ id: "m9" })
    const { user } = renderForm("/catalogo/modelos/novo")
    await fillNew(user, "12000")

    await user.click(save())

    expect(await screen.findByText("lista de modelos")).toBeInTheDocument()
    expect(service.registerEquipmentModel).toHaveBeenCalledWith(
      {
        manufacturerId: "f-b",
        typeId: "t-ac",
        name: "Split Inverter 12.000 BTU",
        values: [{ definitionId: "c-capacity", amount: "12000" }],
      },
      expect.anything(),
    )
  })

  it("característica opcional vazia envia sem valor", async () => {
    vi.mocked(service.listEquipmentTypes).mockResolvedValue(
      page([
        {
          ...AIR_CONDITIONER,
          characteristics: [{ ...AIR_CONDITIONER.characteristics[0], required: false }],
        },
      ]),
    )
    vi.mocked(service.registerEquipmentModel).mockResolvedValue({ id: "m9" })
    const { user } = renderForm("/catalogo/modelos/novo")
    await screen.findByRole("option", { name: "Ar-condicionado" })
    await user.selectOptions(screen.getByLabelText("Fabricante *"), "Huawei")
    await user.selectOptions(screen.getByLabelText("Tipo *"), "Ar-condicionado")
    await user.type(screen.getByLabelText("Nome do modelo *"), "Split")

    await user.click(save())

    await waitFor(() =>
      expect(service.registerEquipmentModel).toHaveBeenCalledWith(
        expect.objectContaining({ values: [] }),
        expect.anything(),
      ),
    )
  })

  it("model_name_taken sai sob o nome, com ícone, mantendo o texto digitado", async () => {
    vi.mocked(service.registerEquipmentModel).mockRejectedValue({
      status: 409,
      code: "model_name_taken",
      message: "x",
    })
    const { user } = renderForm("/catalogo/modelos/novo")
    await fillNew(user, "12000")

    await user.click(save())

    const name = screen.getByLabelText("Nome do modelo *")
    await waitFor(() => expect(name).toHaveAttribute("aria-invalid", "true"))
    expect(name).toHaveValue("Split Inverter 12.000 BTU")
    expect(name).toHaveAccessibleDescription(
      "este fabricante já tem um modelo com esse nome",
    )
    const message = screen.getByText("este fabricante já tem um modelo com esse nome")
    expect(message).toHaveClass("text-body-sm", "text-danger-text")
    expect(message.querySelector("svg")).toBeInTheDocument()
    expect(screen.queryByRole("alert")).not.toBeInTheDocument()
  })

  it("characteristic_required sai no valor, com o nome da característica", async () => {
    vi.mocked(service.registerEquipmentModel).mockRejectedValue({
      status: 422,
      code: "characteristic_required",
      message: "x",
    })
    const { user } = renderForm("/catalogo/modelos/novo")
    await fillNew(user, "12000")

    await user.click(save())

    await waitFor(() =>
      expect(screen.getByLabelText("Capacidade *")).toHaveAccessibleDescription(
        /informe a característica Capacidade/,
      ),
    )
  })

  it("invalid_amount sai no valor", async () => {
    vi.mocked(service.registerEquipmentModel).mockRejectedValue({
      status: 400,
      code: "invalid_amount",
      message: "x",
    })
    const { user } = renderForm("/catalogo/modelos/novo")
    await fillNew(user, "12000")

    await user.click(save())

    await waitFor(() =>
      expect(screen.getByLabelText("Capacidade *")).toHaveAccessibleDescription(
        /o valor precisa ser maior que zero, com até 3 casas decimais/,
      ),
    )
    expect(screen.getByLabelText("Capacidade *")).toHaveFocus()
  })

  it("characteristic_required leva o foco ao valor", async () => {
    vi.mocked(service.registerEquipmentModel).mockRejectedValue({
      status: 422,
      code: "characteristic_required",
      message: "x",
    })
    const { user } = renderForm("/catalogo/modelos/novo")
    await fillNew(user, "12000")

    await user.click(save())

    await waitFor(() => expect(screen.getByLabelText("Capacidade *")).toHaveFocus())
  })

  it.each([
    ["manufacturer_deactivated", "Este fabricante está desativado"],
    ["type_deactivated", "Este tipo está desativado"],
  ])("%s sai no aviso", async (code, text) => {
    vi.mocked(service.registerEquipmentModel).mockRejectedValue({
      status: 422,
      code,
      message: "x",
    })
    const { user } = renderForm("/catalogo/modelos/novo")
    await fillNew(user, "12000")

    await user.click(save())

    expect(await screen.findByRole("alert")).toHaveTextContent(text)
    expect(screen.getByLabelText("Nome do modelo *")).toHaveValue(
      "Split Inverter 12.000 BTU",
    )
  })

  it("Técnico: a rota de novo modelo volta para a lista", async () => {
    renderForm("/catalogo/modelos/novo", false)

    expect(await screen.findByText("lista de modelos")).toBeInTheDocument()
  })

  it("celular: cabeçalho próprio com Voltar para a lista", async () => {
    renderForm("/catalogo/modelos/novo")

    expect(
      await screen.findByRole("heading", { level: 1, name: "Novo modelo" }),
    ).toHaveClass("text-title")
    expect(screen.getByRole("link", { name: "Voltar" })).toHaveAttribute(
      "href",
      "/catalogo",
    )
  })

  it("web: título display, link Modelos e Cancelar", async () => {
    setDesktop(true)
    renderForm("/catalogo/modelos/novo")

    expect(
      await screen.findByRole("heading", { level: 1, name: "Novo modelo" }),
    ).toHaveClass("text-display-lg")
    expect(screen.getByRole("link", { name: "Modelos" })).toHaveAttribute(
      "href",
      "/catalogo",
    )
    expect(screen.getByRole("link", { name: "Cancelar" })).toHaveAttribute(
      "href",
      "/catalogo",
    )
    // `md:text-body-sm` traz o peso 400 do token; o botão é 600 (mockup 3).
    expect(screen.getByRole("button", { name: "Salvar modelo" })).toHaveClass(
      "md:font-semibold",
    )
  })
})

describe("editar modelo", () => {
  it("fabricante e tipo são texto, não campo; nome e valor vêm preenchidos", async () => {
    renderForm("/catalogo/modelos/m1")

    expect(await screen.findByLabelText("Potência *")).toHaveValue("3")
    expect(screen.getByLabelText("Nome do modelo *")).toHaveValue("SUN2000-3KTL-L1")
    expect(screen.queryByRole("combobox")).not.toBeInTheDocument()
    expect(screen.getByText("Huawei")).toBeInTheDocument()
    expect(screen.getByText("Inversor")).toBeInTheDocument()
    expect(service.listEquipmentTypes).toHaveBeenCalledWith({
      status: "ALL",
      page: 0,
      size: 100,
    })
  })

  it("Salvar alterações envia o nome e o valor", async () => {
    vi.mocked(service.updateEquipmentModel).mockResolvedValue()
    const { user } = renderForm("/catalogo/modelos/m1")
    const amount = await screen.findByLabelText("Potência *")
    await user.clear(amount)
    await user.type(amount, "3,5")

    await user.click(save("Salvar alterações"))

    await waitFor(() =>
      expect(service.updateEquipmentModel).toHaveBeenCalledWith("m1", {
        name: "SUN2000-3KTL-L1",
        values: [{ definitionId: "c-power", amount: "3.5" }],
      }),
    )
  })

  it("concurrent_update sai no aviso, sem perder o que foi digitado", async () => {
    vi.mocked(service.updateEquipmentModel).mockRejectedValue({
      status: 409,
      code: "concurrent_update",
      message: "x",
    })
    const { user } = renderForm("/catalogo/modelos/m1")
    const amount = await screen.findByLabelText("Potência *")
    await user.clear(amount)
    await user.type(amount, "4")

    await user.click(save("Salvar alterações"))

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "O registro foi alterado ao mesmo tempo por outra operação; tente de novo",
    )
    expect(amount).toHaveValue("4")
  })

  it("Desativar modelo chama o serviço, sem confirmação, e a tela passa a Reativar modelo", async () => {
    vi.mocked(service.deactivateEquipmentModel).mockImplementation(async () => {
      vi.mocked(service.getEquipmentModel).mockResolvedValue({
        ...SUN,
        status: "DEACTIVATED",
      })
    })
    const { user } = renderForm("/catalogo/modelos/m1")

    await user.click(await screen.findByRole("button", { name: "Desativar modelo" }))

    expect(service.deactivateEquipmentModel).toHaveBeenCalledWith("m1", expect.anything())
    expect(
      await screen.findByRole("button", { name: "Reativar modelo" }),
    ).toBeInTheDocument()
    expect(screen.queryByRole("textbox")).not.toBeInTheDocument()
  })

  it("model_deactivated refaz a consulta e a tela passa a só leitura, com Reativar modelo", async () => {
    vi.mocked(service.updateEquipmentModel).mockImplementation(async () => {
      vi.mocked(service.getEquipmentModel).mockResolvedValue({
        ...SUN,
        status: "DEACTIVATED",
      })
      throw { status: 422, code: "model_deactivated", message: "x" }
    })
    const { user } = renderForm("/catalogo/modelos/m1")
    await screen.findByLabelText("Potência *")

    await user.click(save("Salvar alterações"))

    expect(
      await screen.findByRole("button", { name: "Reativar modelo" }),
    ).toBeInTheDocument()
    expect(screen.queryByRole("textbox")).not.toBeInTheDocument()
  })

  it("desativado: só leitura, com o valor e Reativar modelo", async () => {
    vi.mocked(service.getEquipmentModel).mockResolvedValue({
      ...SUN,
      status: "DEACTIVATED",
    })
    vi.mocked(service.reactivateEquipmentModel).mockResolvedValue()
    const { user } = renderForm("/catalogo/modelos/m1")

    expect(await screen.findByText("3 kW")).toBeInTheDocument()
    expect(screen.queryByRole("textbox")).not.toBeInTheDocument()
    expect(screen.getByText("Desativado").querySelector("svg")).toHaveAttribute(
      "data-shape",
      "ring",
    )
    expect(screen.getByText(/Para editar, reative primeiro/)).toBeInTheDocument()

    await user.click(screen.getByRole("button", { name: "Reativar modelo" }))

    expect(service.reactivateEquipmentModel).toHaveBeenCalledWith("m1", expect.anything())
  })

  it("Técnico: abre só leitura, sem botão de escrita", async () => {
    renderForm("/catalogo/modelos/m1", false)

    expect(await screen.findByText("3 kW")).toBeInTheDocument()
    expect(screen.queryByRole("textbox")).not.toBeInTheDocument()
    expect(screen.queryByRole("button")).not.toBeInTheDocument()
  })

  it("model_not_found sai no aviso", async () => {
    vi.mocked(service.getEquipmentModel).mockRejectedValue({
      status: 404,
      code: "model_not_found",
      message: "x",
    })
    renderForm("/catalogo/modelos/m1")

    expect(await screen.findByRole("alert")).toHaveTextContent("Modelo não encontrado")
  })

  it("a consulta dos tipos que volta sem o tipo do modelo não apaga o valor digitado", async () => {
    const { user, client } = renderForm("/catalogo/modelos/m1")
    const amount = await screen.findByLabelText("Potência *")
    await user.clear(amount)
    await user.type(amount, "4")
    vi.mocked(service.listEquipmentTypes).mockResolvedValue(page([AIR_CONDITIONER]))

    await act(() => client.invalidateQueries({ queryKey: ["catalog", "types"] }))

    expect(screen.getByLabelText("Potência *")).toHaveValue("4")
  })

  it.each([
    ["pendente", () => new Promise<never>(() => undefined)],
    [
      "com falha",
      () => Promise.reject({ status: 403, code: "forbidden", message: "Forbidden" }),
    ],
  ])(
    "consulta dos tipos %s: o campo vem do valor salvo e Salvar envia o valor",
    async (_, types) => {
      vi.mocked(service.listEquipmentTypes).mockImplementation(types)
      vi.mocked(service.updateEquipmentModel).mockResolvedValue()
      const { user } = renderForm("/catalogo/modelos/m1")
      const amount = await screen.findByLabelText("Potência *")
      expect(amount).toHaveValue("3")
      await user.clear(amount)
      await user.type(amount, "4")

      await user.click(save("Salvar alterações"))

      await waitFor(() =>
        expect(service.updateEquipmentModel).toHaveBeenCalledWith("m1", {
          name: "SUN2000-3KTL-L1",
          values: [{ definitionId: "c-power", amount: "4" }],
        }),
      )
    },
  )

  it("modelo sem valor salvo e tipos pendentes: Salvar fica desligado, sem enviar values vazio", async () => {
    vi.mocked(service.getEquipmentModel).mockResolvedValue({ ...SUN, values: [] })
    vi.mocked(service.listEquipmentTypes).mockReturnValue(new Promise(() => undefined))
    renderForm("/catalogo/modelos/m1")

    await screen.findByLabelText("Nome do modelo *")

    expect(save("Salvar alterações")).toBeDisabled()
    expect(screen.getByRole("status")).toHaveAttribute("aria-busy", "true")
    expect(screen.queryByLabelText(/Potência/)).not.toBeInTheDocument()
  })

  it("modelo sem valor salvo e tipos com falha: o aviso aparece e Salvar fica desligado", async () => {
    vi.mocked(service.getEquipmentModel).mockResolvedValue({ ...SUN, values: [] })
    vi.mocked(service.listEquipmentTypes).mockRejectedValue({
      status: 403,
      code: "forbidden",
      message: "Forbidden",
    })
    renderForm("/catalogo/modelos/m1")

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Você não tem permissão para isto",
    )
    expect(save("Salvar alterações")).toBeDisabled()
  })

  it("apagar de propósito o valor opcional envia values vazio", async () => {
    vi.mocked(service.listEquipmentTypes).mockResolvedValue(
      page([
        {
          ...INVERTER,
          characteristics: [{ ...INVERTER.characteristics[0], required: false }],
        },
      ]),
    )
    vi.mocked(service.updateEquipmentModel).mockResolvedValue()
    const { user } = renderForm("/catalogo/modelos/m1")
    const amount = await screen.findByLabelText("Potência")

    await user.clear(amount)
    await user.click(save("Salvar alterações"))

    await waitFor(() =>
      expect(service.updateEquipmentModel).toHaveBeenCalledWith("m1", {
        name: "SUN2000-3KTL-L1",
        values: [],
      }),
    )
  })

  it("characteristic_required sem o campo na tela sai no aviso", async () => {
    vi.mocked(service.getEquipmentModel).mockResolvedValue({ ...SUN, values: [] })
    vi.mocked(service.listEquipmentTypes).mockResolvedValue(
      page([{ ...INVERTER, characteristics: [] }]),
    )
    vi.mocked(service.updateEquipmentModel).mockRejectedValue({
      status: 422,
      code: "characteristic_required",
      message: "informe a característica Potência",
    })
    const { user } = renderForm("/catalogo/modelos/m1")
    await waitFor(() => expect(save("Salvar alterações")).toBeEnabled())

    await user.click(save("Salvar alterações"))

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Informe a característica obrigatória",
    )
  })

  it("web: o nome do modelo é o título e o painel de situação fica ao lado", async () => {
    setDesktop(true)
    renderForm("/catalogo/modelos/m1")

    expect(
      await screen.findByRole("heading", { level: 1, name: "SUN2000-3KTL-L1" }),
    ).toHaveClass("text-display-lg")
    expect(
      screen.getByRole("complementary", { name: "Situação do modelo" }),
    ).toBeInTheDocument()
  })

  it("web: Reativar modelo fica em peso 600", async () => {
    setDesktop(true)
    vi.mocked(service.getEquipmentModel).mockResolvedValue({
      ...SUN,
      status: "DEACTIVATED",
    })
    renderForm("/catalogo/modelos/m1")

    expect(await screen.findByRole("button", { name: "Reativar modelo" })).toHaveClass(
      "md:font-semibold",
    )
  })
})
