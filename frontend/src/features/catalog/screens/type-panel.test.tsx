import { QueryClientProvider } from "@tanstack/react-query"
import { render, screen, waitFor } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import { createRoutesStub, Outlet } from "react-router"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"

import { createQueryClient } from "@/shared/lib/query-client"

import * as service from "../catalog.service"
import type { EquipmentTypeSummary } from "../catalog.service"
import { equipmentTypeSchema } from "../catalog.schemas"
import { TypePanel } from "./type-panel"

vi.mock("../catalog.service")

const INVERSOR: EquipmentTypeSummary = {
  id: "t-inv",
  name: "Inversor",
  status: "ACTIVE",
  modelCount: 3,
  characteristics: [{ id: "c-pot", name: "Potência", unit: "KILOWATT", required: true }],
}
const PAINEL: EquipmentTypeSummary = {
  id: "t-painel",
  name: "Painel solar",
  status: "ACTIVE",
  modelCount: 0,
  characteristics: [
    { id: "c-pico", name: "Potência de pico", unit: "WATT_PEAK", required: false },
  ],
}

const page = (content: EquipmentTypeSummary[]) => ({
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
        { path: "/catalogo/tipos", Component: () => <p>lista de tipos</p> },
        {
          path: "/catalogo/tipos/novo",
          Component: () => <TypePanel mode="new" canEdit={canEdit} />,
        },
        {
          path: "/catalogo/tipos/:id",
          Component: () => <TypePanel mode="edit" canEdit={canEdit} />,
        },
      ],
    },
  ])

  render(<Stub initialEntries={[path]} />)

  return userEvent.setup()
}

const originalMatchMedia = window.matchMedia
const button = (name: string) => screen.getByRole("button", { name })
const typeName = () => screen.findByLabelText("Nome do tipo *")
const characteristicName = () => screen.getByLabelText("Nome da característica *")
const unit = () => screen.getByLabelText("Unidade *")
const required = () => screen.getByLabelText("Obrigatória no cadastro do modelo")
const NOTICE = "A unidade não muda porque o tipo já tem modelos cadastrados"

beforeEach(() => {
  vi.resetAllMocks()
  vi.mocked(service.listEquipmentTypes).mockResolvedValue(page([INVERSOR, PAINEL]))
})

afterEach(() => {
  window.matchMedia = originalMatchMedia
})

describe("Novo tipo", () => {
  it("Salvar tipo envia a característica com a unidade pelo nome do enum e volta para a lista", async () => {
    vi.mocked(service.registerEquipmentType).mockResolvedValue({ id: "t-new" })
    const user = renderPanel("/catalogo/tipos/novo")

    await user.type(await typeName(), "  Painel solar  ")
    await user.type(characteristicName(), "Potência de pico")
    await user.selectOptions(unit(), "Wp")
    await user.click(button("Salvar tipo"))

    await waitFor(() =>
      expect(service.registerEquipmentType).toHaveBeenCalledWith(
        {
          name: "Painel solar",
          characteristic: { name: "Potência de pico", unit: "WATT_PEAK", required: true },
        },
        expect.anything(),
      ),
    )
    expect(await screen.findByText("lista de tipos")).toBeInTheDocument()
  })

  it("a unidade é uma lista fechada de quatro, sem nenhuma escolhida, e a caixa vem marcada", async () => {
    renderPanel("/catalogo/tipos/novo")
    await typeName()

    expect(
      Array.from(unit().querySelectorAll("option")).map((option) => option.textContent),
    ).toEqual(["Escolha a unidade", "kW", "W", "Wp", "BTU/h"])
    expect(unit()).toHaveValue("")
    expect(required()).toBeChecked()
  })

  it("desmarcar Obrigatória envia required false", async () => {
    vi.mocked(service.registerEquipmentType).mockResolvedValue({ id: "t-new" })
    const user = renderPanel("/catalogo/tipos/novo")
    await user.type(await typeName(), "Bateria")
    await user.type(characteristicName(), "Capacidade")
    await user.selectOptions(unit(), "kW")
    await user.click(required())

    await user.click(button("Salvar tipo"))

    await waitFor(() =>
      expect(service.registerEquipmentType).toHaveBeenCalledWith(
        {
          name: "Bateria",
          characteristic: { name: "Capacidade", unit: "KILOWATT", required: false },
        },
        expect.anything(),
      ),
    )
  })

  it("sem unidade escolhida o envio é recusado, sem chamar o serviço", async () => {
    const user = renderPanel("/catalogo/tipos/novo")
    await user.type(await typeName(), "Bateria")
    await user.type(characteristicName(), "Capacidade")

    await user.click(button("Salvar tipo"))

    expect(await screen.findByText("escolha a unidade")).toBeVisible()
    expect(unit()).toHaveAttribute("aria-invalid", "true")
    expect(service.registerEquipmentType).not.toHaveBeenCalled()
  })

  it("type_name_taken sai sob o nome do tipo, com ícone e texto, e o texto digitado fica", async () => {
    vi.mocked(service.registerEquipmentType).mockRejectedValue({
      status: 409,
      code: "type_name_taken",
      message: "x",
    })
    const user = renderPanel("/catalogo/tipos/novo")
    const name = await typeName()
    await user.type(name, "Inversor")
    await user.type(characteristicName(), "Potência")
    await user.selectOptions(unit(), "kW")

    await user.click(button("Salvar tipo"))

    const error = await screen.findByText("já existe um tipo com este nome")
    expect(error.querySelector("svg")).toBeInTheDocument()
    expect(name).toHaveValue("Inversor")
    expect(name).toHaveAttribute("aria-invalid", "true")
    expect(name).toHaveAccessibleDescription(/já existe um tipo com este nome/)
    expect(screen.queryByRole("alert")).not.toBeInTheDocument()
  })

  it("concurrent_update sai no aviso, sem perder o que foi digitado", async () => {
    vi.mocked(service.registerEquipmentType).mockRejectedValue({
      status: 409,
      code: "concurrent_update",
      message: "x",
    })
    const user = renderPanel("/catalogo/tipos/novo")
    const name = await typeName()
    await user.type(name, "Bateria")
    await user.type(characteristicName(), "Capacidade")
    await user.selectOptions(unit(), "kW")

    await user.click(button("Salvar tipo"))

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "O registro foi alterado ao mesmo tempo por outra operação; tente de novo",
    )
    expect(name).toHaveValue("Bateria")
    expect(characteristicName()).toHaveValue("Capacidade")
  })

  it("os limites são os do domínio: tipo de 2 a 60, característica de 2 a 40", async () => {
    const user = renderPanel("/catalogo/tipos/novo")
    const name = await typeName()

    await user.type(name, "a".repeat(65))
    await user.type(characteristicName(), "b".repeat(45))

    expect(name).toHaveAttribute("maxlength", "60")
    expect(name).toHaveValue("a".repeat(60))
    expect(characteristicName()).toHaveAttribute("maxlength", "40")
    expect(characteristicName()).toHaveValue("b".repeat(40))

    const parse = (n: string, c: string) =>
      equipmentTypeSchema.safeParse({
        name: n,
        characteristicName: c,
        unit: "WATT",
        required: true,
      }).success
    expect(parse("a".repeat(60), "b".repeat(40))).toBe(true)
    expect(parse("a".repeat(61), "bb")).toBe(false)
    expect(parse("a", "bb")).toBe(false)
    expect(parse("aa", "b".repeat(41))).toBe(false)
    expect(parse("aa", "b")).toBe(false)
  })

  it("um caractere é recusado pelo mínimo de 2, no tipo e na característica, sem chamar o serviço", async () => {
    const user = renderPanel("/catalogo/tipos/novo")

    await user.type(await typeName(), "B")
    await user.type(characteristicName(), "C")
    await user.click(button("Salvar tipo"))

    expect(await screen.findByText("o nome tem no mínimo 2 caracteres")).toBeVisible()
    expect(
      screen.getByText("o nome da característica tem no mínimo 2 caracteres"),
    ).toBeVisible()
    expect(service.registerEquipmentType).not.toHaveBeenCalled()
  })

  it("no celular o título é o do cabeçalho e Salvar tipo fica fixo no rodapé", async () => {
    renderPanel("/catalogo/tipos/novo")

    expect(
      await screen.findByRole("heading", { level: 1, name: "Novo tipo" }),
    ).toBeInTheDocument()
    expect(button("Salvar tipo").parentElement).toHaveClass("fixed", "bottom-0")
  })

  it("Técnico: a rota volta para a lista", async () => {
    renderPanel("/catalogo/tipos/novo", false)

    expect(await screen.findByText("lista de tipos")).toBeInTheDocument()
    expect(screen.queryByRole("textbox")).not.toBeInTheDocument()
  })
})

describe("Editar tipo", () => {
  it("vem com os campos preenchidos", async () => {
    renderPanel("/catalogo/tipos/t-painel")

    expect(await typeName()).toHaveValue("Painel solar")
    expect(characteristicName()).toHaveValue("Potência de pico")
    expect(unit()).toHaveValue("WATT_PEAK")
    expect(required()).not.toBeChecked()
  })

  it("com modelos a unidade e a caixa Obrigatória ficam desabilitadas, com o aviso do motivo", async () => {
    renderPanel("/catalogo/tipos/t-inv")

    expect(await typeName()).toBeEnabled()
    expect(characteristicName()).toBeEnabled()
    expect(unit()).toBeDisabled()
    expect(unit()).toHaveValue("KILOWATT")
    expect(required()).toBeDisabled()
    expect(required()).toBeChecked()
    expect(screen.getByText(`${NOTICE}.`)).toBeVisible()
    expect(unit()).toHaveAccessibleDescription(`${NOTICE}.`)
  })

  it("sem modelos a unidade e a caixa ficam habilitadas e não há aviso", async () => {
    renderPanel("/catalogo/tipos/t-painel")

    expect(await typeName()).toBeEnabled()
    expect(unit()).toBeEnabled()
    expect(required()).toBeEnabled()
    expect(screen.queryByText(new RegExp(NOTICE))).not.toBeInTheDocument()
  })

  it("Salvar alterações envia o formato do cadastro, com a unidade e a caixa de quem tem modelos", async () => {
    vi.mocked(service.updateEquipmentType).mockResolvedValue()
    const user = renderPanel("/catalogo/tipos/t-inv")
    await typeName()
    const characteristic = characteristicName()

    await user.clear(characteristic)
    await user.type(characteristic, "Potência nominal")
    await user.click(button("Salvar alterações"))

    await waitFor(() =>
      expect(service.updateEquipmentType).toHaveBeenCalledWith("t-inv", {
        name: "Inversor",
        characteristic: { name: "Potência nominal", unit: "KILOWATT", required: true },
      }),
    )
    expect(await screen.findByText("lista de tipos")).toBeInTheDocument()
  })

  it("sem modelos dá para trocar a unidade e a caixa", async () => {
    vi.mocked(service.updateEquipmentType).mockResolvedValue()
    const user = renderPanel("/catalogo/tipos/t-painel")
    await typeName()

    await user.selectOptions(unit(), "W")
    await user.click(required())
    await user.click(button("Salvar alterações"))

    await waitFor(() =>
      expect(service.updateEquipmentType).toHaveBeenCalledWith("t-painel", {
        name: "Painel solar",
        characteristic: { name: "Potência de pico", unit: "WATT", required: true },
      }),
    )
  })

  it("characteristic_locked do servidor sai no aviso, com a mensagem do code", async () => {
    vi.mocked(service.updateEquipmentType).mockRejectedValue({
      status: 422,
      code: "characteristic_locked",
      message: "x",
    })
    const user = renderPanel("/catalogo/tipos/t-painel")
    await typeName()
    await user.selectOptions(unit(), "W")

    await user.click(button("Salvar alterações"))

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "O tipo já tem modelos: a unidade e a obrigatoriedade não podem mudar",
    )
  })

  it("characteristic_locked refaz a consulta: a unidade e a caixa travam no valor salvo, e o aviso e o nome digitado ficam", async () => {
    vi.mocked(service.updateEquipmentType).mockImplementation(async () => {
      vi.mocked(service.listEquipmentTypes).mockResolvedValue(
        page([INVERSOR, { ...PAINEL, modelCount: 1 }]),
      )
      throw { status: 422, code: "characteristic_locked", message: "x" }
    })
    const user = renderPanel("/catalogo/tipos/t-painel")
    const name = await typeName()
    await user.clear(name)
    await user.type(name, "Módulo solar")
    await user.selectOptions(unit(), "W")
    await user.click(required())

    await user.click(button("Salvar alterações"))

    await waitFor(() => expect(unit()).toBeDisabled())
    expect(await screen.findByRole("alert")).toHaveTextContent(
      "O tipo já tem modelos: a unidade e a obrigatoriedade não podem mudar",
    )
    expect(name).toHaveValue("Módulo solar")
    expect(unit()).toHaveValue("WATT_PEAK")
    expect(required()).toBeDisabled()
    expect(required()).not.toBeChecked()
  })

  it("type_name_taken ao editar mantém o texto digitado", async () => {
    vi.mocked(service.updateEquipmentType).mockRejectedValue({
      status: 409,
      code: "type_name_taken",
      message: "x",
    })
    const user = renderPanel("/catalogo/tipos/t-inv")
    const name = await typeName()
    await user.clear(name)
    await user.type(name, "Ar-condicionado")

    await user.click(button("Salvar alterações"))

    expect(await screen.findByText("já existe um tipo com este nome")).toBeVisible()
    expect(name).toHaveValue("Ar-condicionado")
  })

  it("type_deactivated sai no aviso e a consulta é refeita: a tela passa a Reativar tipo", async () => {
    vi.mocked(service.updateEquipmentType).mockRejectedValue({
      status: 422,
      code: "type_deactivated",
      message: "x",
    })
    const user = renderPanel("/catalogo/tipos/t-inv")
    await typeName()
    vi.mocked(service.listEquipmentTypes).mockResolvedValue(
      page([{ ...INVERSOR, status: "DEACTIVATED" }]),
    )

    await user.click(button("Salvar alterações"))

    expect(await screen.findByRole("button", { name: "Reativar tipo" })).toBeVisible()
  })

  it("Desativar tipo chama o serviço, sem confirmação, e a tela passa a Reativar tipo", async () => {
    vi.mocked(service.deactivateEquipmentType).mockImplementation(async () => {
      vi.mocked(service.listEquipmentTypes).mockResolvedValue(
        page([{ ...INVERSOR, status: "DEACTIVATED" }]),
      )
    })
    const user = renderPanel("/catalogo/tipos/t-inv")

    await user.click(await screen.findByRole("button", { name: "Desativar tipo" }))

    expect(service.deactivateEquipmentType).toHaveBeenCalledWith(
      "t-inv",
      expect.anything(),
    )
    expect(await screen.findByRole("button", { name: "Reativar tipo" })).toBeVisible()
    expect(screen.queryByRole("button", { name: "Desativar tipo" })).toBeNull()
  })

  it("desativado: campos só leitura, a situação por forma e só Reativar tipo", async () => {
    vi.mocked(service.listEquipmentTypes).mockResolvedValue(
      page([{ ...INVERSOR, status: "DEACTIVATED" }]),
    )
    vi.mocked(service.reactivateEquipmentType).mockImplementation(async () => {
      vi.mocked(service.listEquipmentTypes).mockResolvedValue(page([INVERSOR]))
    })
    const user = renderPanel("/catalogo/tipos/t-inv")

    const name = await typeName()
    expect(name).toBeDisabled()
    expect(name).toHaveValue("Inversor")
    expect(characteristicName()).toBeDisabled()
    expect(unit()).toBeDisabled()
    expect(required()).toBeDisabled()
    expect(screen.getByText("Desativado").querySelector("svg")).toHaveAttribute(
      "data-shape",
      "ring",
    )
    expect(screen.queryByRole("button", { name: "Salvar alterações" })).toBeNull()
    expect(screen.queryByRole("button", { name: "Desativar tipo" })).toBeNull()

    await user.click(button("Reativar tipo"))

    expect(service.reactivateEquipmentType).toHaveBeenCalledWith(
      "t-inv",
      expect.anything(),
    )
    expect(await screen.findByRole("button", { name: "Desativar tipo" })).toBeVisible()
    expect(await typeName()).toBeEnabled()
  })

  it("erro ao desativar sai no aviso, pelo code", async () => {
    vi.mocked(service.deactivateEquipmentType).mockRejectedValue({
      status: 404,
      code: "type_not_found",
      message: "x",
    })
    const user = renderPanel("/catalogo/tipos/t-inv")

    await user.click(await screen.findByRole("button", { name: "Desativar tipo" }))

    expect(await screen.findByRole("alert")).toHaveTextContent("Tipo não encontrado")
  })

  it("id que a lista não tem: type_not_found no aviso", async () => {
    renderPanel("/catalogo/tipos/outro")

    expect(await screen.findByRole("alert")).toHaveTextContent("Tipo não encontrado")
  })

  it("Técnico: a rota volta para a lista, sem painel nem botão", async () => {
    renderPanel("/catalogo/tipos/t-inv", false)

    expect(await screen.findByText("lista de tipos")).toBeInTheDocument()
    expect(screen.queryByRole("button")).not.toBeInTheDocument()
  })
})

describe("web", () => {
  beforeEach(() => setDesktop(true))

  it("o título é o h2 do painel e Salvar fica dentro dele, sem rodapé fixo", async () => {
    renderPanel("/catalogo/tipos/novo")

    expect(
      await screen.findByRole("heading", { level: 2, name: "Novo tipo" }),
    ).toBeInTheDocument()
    expect(screen.queryByRole("link", { name: "Voltar" })).not.toBeInTheDocument()
    expect(button("Salvar tipo").parentElement).not.toHaveClass("fixed")
  })

  it("a edição leva o nome do tipo no título, com Salvar alterações e Desativar tipo dentro do painel", async () => {
    renderPanel("/catalogo/tipos/t-inv")

    expect(
      await screen.findByRole("heading", { level: 2, name: "Editar tipo: Inversor" }),
    ).toBeInTheDocument()
    expect(button("Salvar alterações").parentElement).not.toHaveClass("fixed")
    expect(button("Desativar tipo")).toBeVisible()
  })
})
