import { describe, expect, it } from "vitest"

import {
  activeEquipmentText,
  errorMessage,
  FORM_MESSAGES,
  LABELS,
  listCounts,
  locationsSummary,
  messageFor,
  sentence,
} from "./customers.messages"

// A tabela de erros de docs/system-design/03-customers/usecases.md.
const TABLE: Record<string, string> = {
  forbidden: "você não tem permissão para isto",
  customer_not_found: "cliente não encontrado",
  location_not_found: "local não encontrado",
  installed_equipment_not_found: "equipamento não encontrado",
  model_not_found: "modelo não encontrado",
  customer_document_taken: "já existe um cliente com este documento",
  serial_number_taken: "este modelo já tem um equipamento com esse número de série",
  customer_deactivated: "este cliente está desativado",
  customer_not_deactivated: "este cliente não está desativado",
  location_deactivated: "este local está desativado",
  location_not_deactivated: "este local não está desativado",
  installed_equipment_deactivated: "este equipamento está desativado",
  installed_equipment_not_deactivated: "este equipamento não está desativado",
  model_deactivated: "este modelo está desativado",
  contact_required: "informe pelo menos um contato: e-mail ou telefone",
  trade_name_not_allowed: "nome fantasia só vale para pessoa jurídica",
  invalid_installation_date: "a data de instalação não pode ser depois de hoje",
  concurrent_update:
    "o registro foi alterado ao mesmo tempo por outra operação; tente de novo",
}

describe("messageFor", () => {
  it.each(Object.entries(TABLE))("%s tem a mensagem da tabela", (code, message) => {
    expect(messageFor(code, "outra")).toBe(message)
  })

  it("devolve o fallback para código desconhecido ou ausente", () => {
    expect(messageFor("nao_existe", "tente de novo")).toBe("tente de novo")
    expect(messageFor(undefined, "tente de novo")).toBe("tente de novo")
  })
})

describe("sentence", () => {
  it("inicial maiúscula e ponto final", () => {
    expect(sentence("este cliente está desativado")).toBe("Este cliente está desativado.")
  })

  it("não repete o ponto nem muda o que já é frase", () => {
    expect(sentence("Informe o CEP.")).toBe("Informe o CEP.")
    expect(sentence("")).toBe("")
  })
})

describe("errorMessage", () => {
  it("pelo code, como frase", () => {
    expect(errorMessage({ code: "customer_deactivated", message: "x" })).toBe(
      "Este cliente está desativado.",
    )
  })

  it("sem code conhecido, o texto da API como frase", () => {
    expect(errorMessage({ message: "cep deve ter entre 8 e 8 caracteres" })).toBe(
      "Cep deve ter entre 8 e 8 caracteres.",
    )
    expect(
      errorMessage({ code: "nao_existe", message: "bairro não pode ser vazio" }),
    ).toBe("Bairro não pode ser vazio.")
  })

  it("sem texto nenhum, a mensagem genérica", () => {
    expect(errorMessage({})).toBe("Algo deu errado; tente de novo.")
  })
})

describe("FORM_MESSAGES", () => {
  it("são frases: inicial maiúscula e ponto final", () => {
    for (const message of Object.values(FORM_MESSAGES))
      expect(message).toMatch(/^[A-ZÀ-Ú].*\.$/)
  })
})

describe("LABELS", () => {
  it("traduz situação e unidade", () => {
    expect(LABELS.status).toEqual({ ACTIVE: "Ativo", DEACTIVATED: "Desativado" })
    expect(LABELS.unit).toEqual({
      KILOWATT: "kW",
      WATT: "W",
      WATT_PEAK: "Wp",
      BTU_PER_HOUR: "BTU/h",
    })
  })
})

describe("contagens", () => {
  it("locationsSummary", () => {
    expect(locationsSummary(2, 1)).toBe("2 ativos, 1 desativado")
    expect(locationsSummary(1, 2)).toBe("1 ativo, 2 desativados")
    // Ficha do cliente desativado (tela 12): sem desativado, só os ativos.
    expect(locationsSummary(1, 0)).toBe("1 ativo")
  })

  it("activeEquipmentText", () => {
    expect(activeEquipmentText(3)).toBe("3 equipamentos ativos")
    expect(activeEquipmentText(1)).toBe("1 equipamento ativo")
    expect(activeEquipmentText(0)).toBe("0 equipamentos ativos")
  })

  it("listCounts", () => {
    expect(listCounts(2, 5)).toBe("2 locais · 5 equip.")
    expect(listCounts(1, 3)).toBe("1 local · 3 equip.")
    expect(listCounts(0, 0)).toBe("0 locais · 0 equip.")
  })
})
