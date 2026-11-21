import { LABELS } from "./catalog.messages"
import type { EquipmentModelSummary } from "./catalog.service"

const FORMAT = /^\d{1,9}(,\d{1,3})?$/

export type ParsedAmount = { ok: true; value: string } | { ok: false; reason: string }

/** O que a pessoa digita (vírgula decimal) vira o texto com ponto que a API espera. */
export function parseAmount(input: string): ParsedAmount {
  if (input === "") return { ok: false, reason: "informe o valor" }
  if (!FORMAT.test(input))
    return {
      ok: false,
      // Texto do Review Focus 1 do plano: o ponto de milhar do pt-BR é recusado, não adivinhado.
      reason: "use só números e vírgula, com até 9 inteiros e 3 decimais",
    }

  const value = input.replace(",", ".")

  if (Number(value) <= 0)
    return { ok: false, reason: "o valor precisa ser maior que zero" }

  return { ok: true, value }
}

/** O valor da API na tela: vírgula decimal, sem milhar e sem zeros à direita. */
export function formatAmount(value: string | number): string {
  const text = String(value)
  const trimmed = text.includes(".") ? text.replace(/0+$/, "").replace(/\.$/, "") : text

  return trimmed.replace(".", ",")
}

/** O valor da característica com a unidade ("3 kW", "12000 BTU/h"). */
export const amountText = (value: EquipmentModelSummary["values"][number]) =>
  `${formatAmount(value.amount)} ${LABELS.unit[value.unit]}`
