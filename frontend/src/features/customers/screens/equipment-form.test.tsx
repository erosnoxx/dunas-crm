import { QueryClientProvider } from "@tanstack/react-query"
import { render, screen, waitFor, within } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import { createRoutesStub, Link, Outlet } from "react-router"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"

import { createQueryClient } from "@/shared/lib/query-client"

import * as service from "../customers.service"
import type {
  CustomerDetail,
  EquipmentView,
  LocationView,
  ModelOption,
} from "../customers.service"
import { EquipmentFormScreen } from "./equipment-form"

vi.mock("../customers.service", async (importOriginal) => ({
  ...(await importOriginal<typeof import("../customers.service")>()),
  getCustomer: vi.fn(),
  listModelOptions: vi.fn(),
  installEquipment: vi.fn(),
  updateInstalledEquipment: vi.fn(),
  deactivateInstalledEquipment: vi.fn(),
  reactivateInstalledEquipment: vi.fn(),
}))

// Relógio fixo: "hoje" é 22/11/2026, o `max` da data na tela 17 (linha 104).
const NOW = new Date(2026, 10, 22, 12)

const SUN: EquipmentView = {
  id: "e1",
  serialNumber: "HV2240187590",
  installedOn: "2025-03-12",
  status: "ACTIVE",
  model: { id: "m1", name: "SUN2000-5KTL-L1", status: "ACTIVE" },
  manufacturer: { id: "f1", name: "Huawei" },
  type: { id: "t1", name: "Inversor" },
  characteristic: { name: "Potência", unit: "KILOWATT", amount: "5.000" },
}

const RETIRED: EquipmentView = {
  ...SUN,
  id: "e3",
  serialNumber: "HV2240187563",
  status: "DEACTIVATED",
}

const SOLIS: EquipmentView = {
  ...SUN,
  id: "e4",
  serialNumber: "S6X0001",
  installedOn: null,
  model: { id: "m3", name: "S6-GR1P5K", status: "ACTIVE" },
  manufacturer: { id: "f3", name: "Solis" },
}

const ORPHAN: EquipmentView = { ...SUN, id: "e2", serialNumber: "HV2230099120" }

const ADDRESS = {
  zipCode: "38400128",
  street: "Av. João Pinheiro",
  number: "1450",
  complement: null,
  neighborhood: "Centro",
  city: "Uberlândia",
  state: "MG",
} as const

const STORE: LocationView = {
  id: "l2",
  name: "Loja Centro",
  status: "ACTIVE",
  address: ADDRESS,
  equipment: [SUN, RETIRED],
}

const FACTORY: LocationView = {
  ...STORE,
  id: "l1",
  name: "Fábrica",
  address: { ...ADDRESS, street: "Rua Tiradentes", number: "220", city: "Araguari" },
  equipment: [SOLIS],
}

const CLOSED: LocationView = {
  ...STORE,
  id: "l3",
  name: "Loja Santa Mônica",
  status: "DEACTIVATED",
  equipment: [ORPHAN, { ...RETIRED, id: "e5", serialNumber: "HV0000000005" }],
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

const MODELS: ModelOption[] = [
  {
    id: "m2",
    name: "Split Inverter 12.000 BTU",
    manufacturer: { id: "f2", name: "Fabricante B" },
    type: { id: "t2", name: "Ar-condicionado" },
    values: [{ name: "Capacidade", unit: "BTU_PER_HOUR", amount: 12000 }],
  },
  {
    id: "m1",
    name: "SUN2000-5KTL-L1",
    manufacturer: { id: "f1", name: "Huawei" },
    type: { id: "t1", name: "Inversor" },
    values: [{ name: "Potência", unit: "KILOWATT", amount: "5.000" }],
  },
]

const withEquipment = (location: LocationView, item: EquipmentView): CustomerDetail => ({
  ...SILVA,
  locations: SILVA.locations.map((each) =>
    each.id === location.id
      ? {
          ...each,
          equipment: each.equipment.map((one) => (one.id === item.id ? item : one)),
        }
      : each,
  ),
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
          path: "/clientes/:id/locais/:locationId/equipamentos/novo",
          Component: () => <EquipmentFormScreen mode="new" canEdit={canEdit} />,
        },
        {
          path: "/clientes/:id/equipamentos/:equipmentId",
          Component: () => (
            <>
              <EquipmentFormScreen mode="edit" canEdit={canEdit} />
              <Link to="/clientes/c2/equipamentos/e4">ir para o Solis</Link>
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

const NEW = "/clientes/c2/locais/l2/equipamentos/novo"
const EDIT = "/clientes/c2/equipamentos/e1"
const TAKEN = "Este modelo já tem um equipamento com esse número de série."
const TAKEN_HINT = "Confira a etiqueta ou busque a série na lista de clientes."
const FUTURE = "A data de instalação não pode ser depois de hoje."

const originalMatchMedia = window.matchMedia
const field = (label: string) => screen.getByLabelText(label)
const model = () => field("Modelo *")
const serial = () => field("Número de série *")
const date = () => field("Data de instalação (opcional)")
const button = (name: string) => screen.getByRole("button", { name })

/** Escolhe o modelo (quando o campo existe) e digita série e data. */
async function fill(user: User, values: { serial?: string; date?: string } = {}) {
  await screen.findByLabelText("Número de série *")
  if (screen.queryByLabelText("Modelo *")) {
    await screen.findByRole("option", {
      name: "SUN2000-5KTL-L1 · Huawei · Inversor · 5 kW",
    })
    await user.selectOptions(model(), "m1")
  }
  if (values.serial !== undefined) {
    await user.clear(serial())
    await user.type(serial(), values.serial)
  }
  if (values.date !== undefined) {
    await user.clear(date())
    if (values.date) await user.type(date(), values.date)
  }
}

beforeEach(() => {
  vi.resetAllMocks()
  vi.useFakeTimers({ toFake: ["Date"] })
  vi.setSystemTime(NOW)
  vi.mocked(service.getCustomer).mockResolvedValue(SILVA)
  vi.mocked(service.listModelOptions).mockResolvedValue(MODELS)
})

afterEach(() => {
  vi.useRealTimers()
  window.matchMedia = originalMatchMedia
})

describe("novo equipamento", () => {
  it("a opção mostra modelo · fabricante · tipo · valor unidade, sem separador de milhar", async () => {
    renderForm(NEW)

    expect(
      await screen.findByRole("option", {
        name: "SUN2000-5KTL-L1 · Huawei · Inversor · 5 kW",
      }),
    ).toHaveValue("m1")
    expect(
      screen.getByRole("option", {
        name: "Split Inverter 12.000 BTU · Fabricante B · Ar-condicionado · 12000 BTU/h",
      }),
    ).toHaveValue("m2")
    expect(model()).toHaveValue("")
  })

  it('sem modelo: "Escolha o modelo." sob o modelo, com o foco nele, e nada sai', async () => {
    const user = renderForm(NEW)
    await screen.findByRole("option", { name: /SUN2000/ })
    await user.type(serial(), "HV2240187590")

    await user.click(button("Salvar equipamento"))

    await waitFor(() =>
      expect(model()).toHaveAccessibleDescription(
        expect.stringContaining("Escolha o modelo."),
      ),
    )
    expect(model()).toHaveFocus()
    expect(service.installEquipment).not.toHaveBeenCalled()
  })

  it('sem série: "Informe o número de série." sob a série e nada sai', async () => {
    const user = renderForm(NEW)
    await fill(user)

    await user.click(button("Salvar equipamento"))

    await waitFor(() =>
      expect(serial()).toHaveAccessibleDescription(
        expect.stringContaining("Informe o número de série."),
      ),
    )
    expect(serial()).toHaveFocus()
    expect(service.installEquipment).not.toHaveBeenCalled()
  })

  it("a data de amanhã mostra o erro sob a data e nada sai; o max é hoje", async () => {
    const user = renderForm(NEW)
    await fill(user, { serial: "HV2240187590", date: "2026-11-23" })
    expect(date()).toHaveAttribute("max", "2026-11-22")
    expect(date()).toHaveAttribute("type", "date")

    await user.click(button("Salvar equipamento"))

    await waitFor(() =>
      expect(date()).toHaveAccessibleDescription(expect.stringContaining(FUTURE)),
    )
    expect(date()).toHaveAttribute("aria-invalid", "true")
    expect(date()).toHaveFocus()
    expect(service.installEquipment).not.toHaveBeenCalled()
  })

  it("sem data, envia installedOn null ao local e volta para a ficha", async () => {
    vi.mocked(service.installEquipment).mockResolvedValue({ id: "e9" })
    const user = renderForm(NEW)
    await fill(user, { serial: " HV2240187590 " })

    await user.click(button("Salvar equipamento"))

    expect(await screen.findByText("ficha do cliente")).toBeInTheDocument()
    expect(service.installEquipment).toHaveBeenCalledWith("l2", {
      modelId: "m1",
      serialNumber: "HV2240187590",
      installedOn: null,
    })
  })

  it("com a data de hoje, envia a data como yyyy-MM-dd", async () => {
    vi.mocked(service.installEquipment).mockResolvedValue({ id: "e9" })
    const user = renderForm(NEW)
    await fill(user, { serial: "HV2240187590", date: "2026-11-22" })

    await user.click(button("Salvar equipamento"))

    await waitFor(() =>
      expect(service.installEquipment).toHaveBeenCalledWith("l2", {
        modelId: "m1",
        serialNumber: "HV2240187590",
        installedOn: "2026-11-22",
      }),
    )
  })

  it("série com limite de 60 caracteres", async () => {
    renderForm(NEW)

    expect(await screen.findByLabelText("Número de série *")).toHaveAttribute(
      "maxlength",
      "60",
    )
  })

  it("409 serial_number_taken sai sob a série com o texto extra, com foco e o digitado mantido", async () => {
    vi.mocked(service.installEquipment).mockRejectedValue({
      status: 409,
      code: "serial_number_taken",
      message: "este modelo já tem um equipamento com esse número de série",
    })
    const user = renderForm(NEW)
    await fill(user, { serial: "1031050223290123", date: "2025-03-12" })

    await user.click(button("Salvar equipamento"))

    await waitFor(() =>
      expect(serial()).toHaveAccessibleDescription(
        expect.stringContaining(`${TAKEN} ${TAKEN_HINT}`),
      ),
    )
    expect(serial()).toHaveAttribute("aria-invalid", "true")
    expect(serial()).toHaveFocus()
    expect(serial()).toHaveValue("1031050223290123")
    expect(date()).toHaveValue("2025-03-12")
    expect(model()).toHaveValue("m1")
    expect(screen.queryByRole("alert")).not.toBeInTheDocument()
  })

  it("400 invalid_installation_date sai sob a data, com foco", async () => {
    vi.mocked(service.installEquipment).mockRejectedValue({
      status: 400,
      code: "invalid_installation_date",
      message: "a data de instalação não pode ser depois de hoje",
    })
    const user = renderForm(NEW)
    await fill(user, { serial: "HV2240187590", date: "2026-11-22" })

    await user.click(button("Salvar equipamento"))

    await waitFor(() =>
      expect(date()).toHaveAccessibleDescription(expect.stringContaining(FUTURE)),
    )
    expect(date()).toHaveFocus()
    expect(serial()).toHaveValue("HV2240187590")
  })

  it.each([
    ["model_deactivated", 422, "Este modelo está desativado."],
    ["location_deactivated", 422, "Este local está desativado."],
    ["customer_deactivated", 422, "Este cliente está desativado."],
    [
      "concurrent_update",
      409,
      "O registro foi alterado ao mesmo tempo por outra operação; tente de novo.",
    ],
    [undefined, 400, "Número de série não pode ser vazio."],
  ])("%s sai no aviso, sem perder o digitado", async (code, status, text) => {
    vi.mocked(service.installEquipment).mockRejectedValue({
      status,
      code,
      message: "número de série não pode ser vazio",
    })
    const user = renderForm(NEW)
    await fill(user, { serial: "HV2240187590" })

    await user.click(button("Salvar equipamento"))

    expect(await screen.findByRole("alert")).toHaveTextContent(text)
    expect(serial()).toHaveValue("HV2240187590")
    expect(serial()).not.toHaveAttribute("aria-invalid")
  })

  it("a lista de modelos que falha mostra o aviso e Salvar fica desligado", async () => {
    vi.mocked(service.listModelOptions).mockRejectedValue({
      status: 0,
      message: "Não foi possível conectar ao servidor.",
    })
    renderForm(NEW)

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Não foi possível conectar ao servidor.",
    )
    expect(button("Salvar equipamento")).toBeDisabled()
  })

  it("o formulário só aparece com a ficha lida agora; antes não há Salvar", async () => {
    vi.mocked(service.getCustomer).mockReturnValue(new Promise(() => undefined))
    renderForm(NEW, true, SILVA)

    expect(await screen.findByRole("status")).toHaveAttribute("aria-busy", "true")
    expect(screen.queryByRole("textbox")).not.toBeInTheDocument()
    expect(
      screen.queryByRole("button", { name: "Salvar equipamento" }),
    ).not.toBeInTheDocument()
  })

  it("local desativado volta para a ficha", async () => {
    renderForm("/clientes/c2/locais/l3/equipamentos/novo")

    expect(await screen.findByText("ficha do cliente")).toBeInTheDocument()
    expect(service.installEquipment).not.toHaveBeenCalled()
  })

  it("cliente desativado volta para a ficha, mesmo com o local ativo", async () => {
    vi.mocked(service.getCustomer).mockResolvedValue({ ...SILVA, status: "DEACTIVATED" })
    renderForm(NEW)

    expect(await screen.findByText("ficha do cliente")).toBeInTheDocument()
  })

  it("local de outro cliente ou inexistente: Local não encontrado, sem formulário", async () => {
    renderForm("/clientes/c2/locais/outro/equipamentos/novo")

    expect(await screen.findByRole("alert")).toHaveTextContent("Local não encontrado.")
    expect(screen.queryByRole("textbox")).not.toBeInTheDocument()
  })

  it("Técnico: o novo equipamento volta para a ficha", async () => {
    renderForm(NEW, false)

    expect(await screen.findByText("ficha do cliente")).toBeInTheDocument()
    expect(service.getCustomer).not.toHaveBeenCalled()
  })

  it("celular: cabeçalho próprio, o local curto, a dica curta do modelo e Salvar no rodapé", async () => {
    renderForm(NEW)

    expect(await screen.findByText("Loja Centro · Uberlândia/MG")).toBeInTheDocument()
    expect(
      screen.getByRole("heading", { level: 1, name: "Novo equipamento" }),
    ).toHaveClass("text-title")
    expect(screen.getByRole("link", { name: "Voltar" })).toHaveAttribute(
      "href",
      "/clientes/c2",
    )
    expect(model()).toHaveAccessibleDescription("Só modelos ativos do catálogo.")
    expect(button("Salvar equipamento")).toHaveClass("min-h-12", "w-full")
    expect(screen.queryByRole("link", { name: "Cancelar" })).not.toBeInTheDocument()
  })

  it("web: título, link com o nome do cliente, local com endereço, dicas, nota e Cancelar", async () => {
    setDesktop(true)
    renderForm(NEW)

    expect(
      await screen.findByText("Loja Centro · Av. João Pinheiro, 1450 · Uberlândia/MG"),
    ).toBeInTheDocument()
    expect(
      screen.getByRole("heading", { level: 1, name: "Novo equipamento" }),
    ).toHaveClass("text-display-lg")
    expect(
      screen.getByRole("link", { name: "J. Silva Comércio de Alimentos Ltda" }),
    ).toHaveAttribute("href", "/clientes/c2")
    expect(model()).toHaveAccessibleDescription(
      "Só modelos ativos do catálogo, com fabricante, tipo e característica para distinguir. Não achou? Cadastre em Catálogo, Modelos.",
    )
    expect(serial()).toHaveAccessibleDescription("Copie da etiqueta do equipamento.")
    expect(date()).toHaveAccessibleDescription("Não pode ser depois de hoje.")
    expect(
      screen.getByText(
        "Modelo e local não mudam depois de salvos. A série e a data podem ser corrigidas.",
      ),
    ).toBeInTheDocument()
    expect(screen.getByRole("link", { name: "Cancelar" })).toHaveAttribute(
      "href",
      "/clientes/c2",
    )
    expect(button("Salvar equipamento")).toHaveClass("md:font-semibold")
  })
})

describe("editar equipamento", () => {
  it("antes da ficha lida agora não há campo, Salvar nem Desativar", async () => {
    vi.mocked(service.getCustomer).mockReturnValue(new Promise(() => undefined))
    renderForm(EDIT, true, SILVA)

    expect(await screen.findByRole("status")).toHaveAttribute("aria-busy", "true")
    expect(screen.queryByRole("textbox")).not.toBeInTheDocument()
    expect(
      screen.queryByRole("button", { name: "Salvar alterações" }),
    ).not.toBeInTheDocument()
    expect(
      screen.queryByRole("button", { name: "Desativar equipamento" }),
    ).not.toBeInTheDocument()
    expect(service.updateInstalledEquipment).not.toHaveBeenCalled()
  })

  it("a ficha guardada no cache não preenche o formulário: só a lida agora", async () => {
    renderForm(EDIT, true, withEquipment(STORE, { ...SUN, serialNumber: "DE-ANTES" }))

    expect(await screen.findByLabelText("Número de série *")).toHaveValue("HV2240187590")
  })

  it("vem com os valores do equipamento, modelo e local só leitura e o modelo no título", async () => {
    renderForm(EDIT)

    expect(await screen.findByLabelText("Número de série *")).toHaveValue("HV2240187590")
    expect(date()).toHaveValue("2025-03-12")
    expect(date()).toHaveAttribute("max", "2026-11-22")
    expect(screen.queryByLabelText("Modelo *")).not.toBeInTheDocument()
    expect(screen.queryByRole("combobox")).not.toBeInTheDocument()
    expect(
      screen.getByText("SUN2000-5KTL-L1 · Huawei · Inversor · 5 kW"),
    ).toBeInTheDocument()
    expect(screen.getByText("Loja Centro · Padaria Boa Vista")).toBeInTheDocument()
    expect(
      screen.getByText(
        "Modelo e local não mudam depois de salvos. Errou? Desative e cadastre de novo.",
      ),
    ).toBeInTheDocument()
    expect(serial()).toHaveAccessibleDescription(
      "Corrija só erro de digitação: a série é a da etiqueta.",
    )
    expect(
      screen.getByRole("heading", { level: 1, name: "SUN2000-5KTL-L1" }),
    ).toBeInTheDocument()
    expect(screen.getByText("Ativo")).toBeInTheDocument()
    expect(service.getCustomer).toHaveBeenCalledWith("c2")
  })

  it("Salvar alterações manda só série e data (sem modelo nem local) e volta para a ficha", async () => {
    vi.mocked(service.updateInstalledEquipment).mockResolvedValue()
    const user = renderForm(EDIT)
    await fill(user, { serial: "HV2240187591", date: "" })

    await user.click(button("Salvar alterações"))

    expect(await screen.findByText("ficha do cliente")).toBeInTheDocument()
    expect(service.updateInstalledEquipment).toHaveBeenCalledWith("e1", {
      serialNumber: "HV2240187591",
      installedOn: null,
    })
    expect(service.installEquipment).not.toHaveBeenCalled()
  })

  it("409 serial_number_taken na edição: só a frase da tabela, com foco e o digitado mantido", async () => {
    vi.mocked(service.updateInstalledEquipment).mockRejectedValue({
      status: 409,
      code: "serial_number_taken",
      message: "este modelo já tem um equipamento com esse número de série",
    })
    const user = renderForm(EDIT)
    await fill(user, { serial: "HV2230099120" })

    await user.click(button("Salvar alterações"))

    await waitFor(() => expect(serial()).toHaveAttribute("aria-invalid", "true"))
    expect(serial()).toHaveAccessibleDescription(
      `${TAKEN} Corrija só erro de digitação: a série é a da etiqueta.`,
    )
    expect(screen.queryByText(new RegExp(TAKEN_HINT))).not.toBeInTheDocument()
    expect(serial()).toHaveFocus()
    expect(serial()).toHaveValue("HV2230099120")
  })

  it("data de amanhã na edição: o erro sob a data e nada sai", async () => {
    const user = renderForm(EDIT)
    await fill(user, { date: "2026-11-23" })

    await user.click(button("Salvar alterações"))

    await waitFor(() =>
      expect(date()).toHaveAccessibleDescription(expect.stringContaining(FUTURE)),
    )
    expect(service.updateInstalledEquipment).not.toHaveBeenCalled()
  })

  it.each([
    ["installed_equipment_deactivated", 422, "Este equipamento está desativado."],
    [
      "concurrent_update",
      409,
      "O registro foi alterado ao mesmo tempo por outra operação; tente de novo.",
    ],
  ])("%s ao salvar sai no aviso e o formulário fica", async (code, status, text) => {
    vi.mocked(service.updateInstalledEquipment).mockRejectedValue({
      status,
      code,
      message: "x",
    })
    const user = renderForm(EDIT)
    await fill(user, { serial: "HV-NOVO" })

    await user.click(button("Salvar alterações"))

    expect(await screen.findByRole("alert")).toHaveTextContent(text)
    expect(serial()).toHaveValue("HV-NOVO")
  })

  it("equipamento de outro cliente ou inexistente: Equipamento não encontrado", async () => {
    renderForm("/clientes/c2/equipamentos/outro")

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Equipamento não encontrado.",
    )
    expect(screen.queryByRole("textbox")).not.toBeInTheDocument()
  })

  it("Técnico: a edição volta para a ficha", async () => {
    renderForm(EDIT, false)

    expect(await screen.findByText("ficha do cliente")).toBeInTheDocument()
    expect(service.getCustomer).not.toHaveBeenCalled()
  })

  it("trocar de equipamento recomeça o formulário com os valores do outro", async () => {
    const user = renderForm(EDIT)
    await user.type(await screen.findByLabelText("Número de série *"), "-digitado")

    await user.click(screen.getByRole("link", { name: "ir para o Solis" }))

    await waitFor(() => expect(serial()).toHaveValue("S6X0001"))
    expect(date()).toHaveValue("")
    expect(
      screen.getByRole("heading", { level: 1, name: "S6-GR1P5K" }),
    ).toBeInTheDocument()
  })

  it('celular: "Desativar equipamento" sem confirmação troca a página para "Reativar equipamento"', async () => {
    vi.mocked(service.deactivateInstalledEquipment).mockResolvedValue()
    const user = renderForm(EDIT)
    await screen.findByLabelText("Número de série *")
    vi.mocked(service.getCustomer).mockResolvedValue(
      withEquipment(STORE, { ...SUN, status: "DEACTIVATED" }),
    )

    await user.click(button("Desativar equipamento"))

    expect(
      await screen.findByRole("button", { name: "Reativar equipamento" }),
    ).toBeVisible()
    expect(service.deactivateInstalledEquipment).toHaveBeenCalledWith(
      "e1",
      expect.anything(),
    )
    expect(service.deactivateInstalledEquipment).toHaveBeenCalledTimes(1)
    // Dentro do <form> no celular: o botão não envia o formulário.
    expect(service.updateInstalledEquipment).not.toHaveBeenCalled()
    expect(screen.queryByRole("dialog")).not.toBeInTheDocument()
    expect(screen.queryByRole("textbox")).not.toBeInTheDocument()
    expect(screen.queryByText("ficha do cliente")).not.toBeInTheDocument()
    expect(screen.getByText("Desativado")).toBeInTheDocument()
  })

  it("desativar com erro mostra o aviso e o formulário fica", async () => {
    vi.mocked(service.deactivateInstalledEquipment).mockRejectedValue({
      status: 422,
      code: "installed_equipment_deactivated",
      message: "x",
    })
    const user = renderForm(EDIT)
    await screen.findByLabelText("Número de série *")

    await user.click(button("Desativar equipamento"))

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Este equipamento está desativado.",
    )
    expect(serial()).toHaveValue("HV2240187590")
  })

  it("web: o painel Situação ao lado, com o selo, a ajuda e Desativar equipamento", async () => {
    setDesktop(true)
    renderForm(EDIT)

    expect(
      await screen.findByRole("heading", { level: 1, name: "SUN2000-5KTL-L1" }),
    ).toHaveClass("text-display-lg")
    const status = screen.getByRole("complementary", { name: "Situação do equipamento" })
    expect(status).toHaveClass("md:max-w-160", "xl:w-85", "xl:shrink-0")
    expect(status.parentElement).toHaveClass("flex-col", "xl:flex-row", "xl:items-start")
    expect(within(status).getByText("Ativo")).toBeInTheDocument()
    expect(within(status).getByText(/^Desativar marca o equipamento/)).toBeInTheDocument()
    expect(
      within(status).getByRole("button", { name: "Desativar equipamento" }),
    ).toBeInTheDocument()
    expect(date()).toHaveAccessibleDescription("Não pode ser depois de hoje.")
    expect(screen.getByRole("link", { name: "Cancelar" })).toHaveAttribute(
      "href",
      "/clientes/c2",
    )
    expect(button("Salvar alterações")).toBeInTheDocument()
  })
})

describe("pai desativado (ADR-30)", () => {
  it("o equipamento de local desativado se edita e salva", async () => {
    vi.mocked(service.updateInstalledEquipment).mockResolvedValue()
    const user = renderForm("/clientes/c2/equipamentos/e2")
    await fill(user, { serial: "HV2230099121" })

    await user.click(button("Salvar alterações"))

    expect(await screen.findByText("ficha do cliente")).toBeInTheDocument()
    expect(service.updateInstalledEquipment).toHaveBeenCalledWith("e2", {
      serialNumber: "HV2230099121",
      installedOn: "2025-03-12",
    })
  })

  it("o equipamento de cliente desativado se edita e salva", async () => {
    vi.mocked(service.getCustomer).mockResolvedValue({ ...SILVA, status: "DEACTIVATED" })
    vi.mocked(service.updateInstalledEquipment).mockResolvedValue()
    const user = renderForm(EDIT)
    await screen.findByLabelText("Número de série *")

    await user.click(button("Salvar alterações"))

    expect(await screen.findByText("ficha do cliente")).toBeInTheDocument()
    expect(service.updateInstalledEquipment).toHaveBeenCalledWith("e1", {
      serialNumber: "HV2240187590",
      installedOn: "2025-03-12",
    })
  })

  it("o equipamento de local desativado se desativa", async () => {
    vi.mocked(service.getCustomer).mockResolvedValue({ ...SILVA, status: "DEACTIVATED" })
    vi.mocked(service.deactivateInstalledEquipment).mockResolvedValue()
    const user = renderForm("/clientes/c2/equipamentos/e2")
    await screen.findByLabelText("Número de série *")

    await user.click(button("Desativar equipamento"))

    await waitFor(() =>
      expect(service.deactivateInstalledEquipment).toHaveBeenCalledWith(
        "e2",
        expect.anything(),
      ),
    )
  })

  it("o equipamento desativado de local e cliente desativados se reativa", async () => {
    vi.mocked(service.getCustomer).mockResolvedValue({ ...SILVA, status: "DEACTIVATED" })
    vi.mocked(service.reactivateInstalledEquipment).mockResolvedValue()
    const user = renderForm("/clientes/c2/equipamentos/e5")

    await user.click(await screen.findByRole("button", { name: "Reativar equipamento" }))

    await waitFor(() =>
      expect(service.reactivateInstalledEquipment).toHaveBeenCalledWith(
        "e5",
        expect.anything(),
      ),
    )
  })
})

describe("equipamento desativado", () => {
  it("aberto por URL fica só leitura, com o aviso e Reativar equipamento", async () => {
    renderForm("/clientes/c2/equipamentos/e3")

    expect(
      await screen.findByRole("button", { name: "Reativar equipamento" }),
    ).toBeInTheDocument()
    expect(screen.queryByRole("textbox")).not.toBeInTheDocument()
    expect(
      screen.queryByRole("button", { name: "Salvar alterações" }),
    ).not.toBeInTheDocument()
    expect(
      screen.getByRole("heading", { level: 1, name: "SUN2000-5KTL-L1" }),
    ).toBeInTheDocument()
    const data = screen.getByRole("region", { name: "Dados do equipamento" })
    expect(data).toHaveTextContent("SUN2000-5KTL-L1 · Huawei · Inversor · 5 kW")
    expect(data).toHaveTextContent("Loja Centro · Padaria Boa Vista")
    expect(data).toHaveTextContent("HV2240187563")
    expect(data).toHaveTextContent("12/03/2025")
    expect(screen.getByText("Desativado")).toBeInTheDocument()
    expect(
      screen.getByText(
        "Este equipamento está desativado e continua na ficha, marcado. A série dele segue ocupada para este modelo. Para corrigir a série ou a data, reative primeiro.",
      ),
    ).toBeInTheDocument()
  })

  it("Reativar equipamento volta a página ao formulário, sem sair dela", async () => {
    vi.mocked(service.reactivateInstalledEquipment).mockResolvedValue()
    const user = renderForm("/clientes/c2/equipamentos/e3")
    await screen.findByRole("button", { name: "Reativar equipamento" })
    vi.mocked(service.getCustomer).mockResolvedValue(
      withEquipment(STORE, { ...RETIRED, status: "ACTIVE" }),
    )

    await user.click(button("Reativar equipamento"))

    expect(await screen.findByLabelText("Número de série *")).toHaveValue("HV2240187563")
    expect(service.reactivateInstalledEquipment).toHaveBeenCalledWith(
      "e3",
      expect.anything(),
    )
    expect(screen.queryByRole("dialog")).not.toBeInTheDocument()
    expect(button("Desativar equipamento")).toBeInTheDocument()
  })

  it("reativar com erro mostra o aviso", async () => {
    vi.mocked(service.reactivateInstalledEquipment).mockRejectedValue({
      status: 422,
      code: "installed_equipment_not_deactivated",
      message: "x",
    })
    const user = renderForm("/clientes/c2/equipamentos/e3")

    await user.click(await screen.findByRole("button", { name: "Reativar equipamento" }))

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Este equipamento não está desativado.",
    )
  })

  it("web: os dados à esquerda e Reativar no painel Situação", async () => {
    setDesktop(true)
    renderForm("/clientes/c2/equipamentos/e3")

    const status = await screen.findByRole("complementary", {
      name: "Situação do equipamento",
    })
    expect(status).toHaveClass("md:max-w-160", "xl:w-85", "xl:shrink-0")
    expect(status.parentElement).toHaveClass("flex-col", "xl:flex-row", "xl:items-start")
    expect(screen.getByRole("region", { name: "Dados do equipamento" })).toHaveClass(
      "md:w-160",
      "md:max-w-full",
    )
    expect(within(status).getByText("Desativado")).toBeInTheDocument()
    expect(
      within(status).getByRole("button", { name: "Reativar equipamento" }),
    ).toBeInTheDocument()
    expect(
      screen.getByRole("region", { name: "Dados do equipamento" }),
    ).toHaveTextContent("HV2240187563")
  })

  it("Técnico: o equipamento desativado também volta para a ficha", async () => {
    renderForm("/clientes/c2/equipamentos/e3", false)

    expect(await screen.findByText("ficha do cliente")).toBeInTheDocument()
  })
})
