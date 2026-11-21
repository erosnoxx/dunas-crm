import { describe, expect, it } from "vitest"

import { LABELS, messageFor } from "./catalog.messages"

// A tabela de erros de docs/system-design/02-catalog/usecases.md.
const TABLE: Record<string, string> = {
  forbidden: "você não tem permissão para isto",
  manufacturer_not_found: "fabricante não encontrado",
  type_not_found: "tipo não encontrado",
  model_not_found: "modelo não encontrado",
  manufacturer_name_taken: "já existe um fabricante com este nome",
  type_name_taken: "já existe um tipo com este nome",
  model_name_taken: "este fabricante já tem um modelo com esse nome",
  manufacturer_deactivated: "este fabricante está desativado",
  manufacturer_not_deactivated: "este fabricante não está desativado",
  type_deactivated: "este tipo está desativado",
  type_not_deactivated: "este tipo não está desativado",
  model_deactivated: "este modelo está desativado",
  model_not_deactivated: "este modelo não está desativado",
  characteristic_locked:
    "o tipo já tem modelos: a unidade e a obrigatoriedade não podem mudar",
  characteristic_unknown: "a característica informada não existe neste tipo",
  // A tabela traz "informe a característica <nome>": o nome só a resposta da API conhece.
  characteristic_required: "informe a característica obrigatória",
  invalid_amount: "o valor precisa ser maior que zero, com até 3 casas decimais",
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

describe("LABELS", () => {
  it("traduz unidade e situação", () => {
    expect(LABELS.unit).toEqual({
      KILOWATT: "kW",
      WATT: "W",
      WATT_PEAK: "Wp",
      BTU_PER_HOUR: "BTU/h",
    })
    expect(LABELS.status).toEqual({ ACTIVE: "Ativo", DEACTIVATED: "Desativado" })
  })
})
