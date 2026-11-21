import { UNKNOWN_ERROR_MESSAGE, type ApiError } from "@/shared/api/api-error"

import type { CatalogStatus, Unit } from "./catalog.service"

/**
 * Mensagens da tabela de erros de docs/system-design/02-catalog/usecases.md, por `code`.
 * `characteristic_required` perde o `<nome>` da tabela: quem o conhece é a resposta da API.
 */
const MESSAGES: Record<string, string> = {
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
  characteristic_required: "informe a característica obrigatória",
  invalid_amount: "o valor precisa ser maior que zero, com até 3 casas decimais",
  concurrent_update:
    "o registro foi alterado ao mesmo tempo por outra operação; tente de novo",
}

export function messageFor(code: string | undefined, fallback: string): string {
  return (code !== undefined && MESSAGES[code]) || fallback
}

/** A mensagem de um erro da API para o aviso: pelo `code`, senão a da própria API; a frase começa com maiúscula. */
export function errorMessage(error: Partial<ApiError>): string {
  const text = messageFor(error.code, error.message || UNKNOWN_ERROR_MESSAGE)

  return text.charAt(0).toUpperCase() + text.slice(1)
}

export const LABELS: {
  unit: Record<Unit, string>
  status: Record<CatalogStatus, string>
} = {
  unit: { KILOWATT: "kW", WATT: "W", WATT_PEAK: "Wp", BTU_PER_HOUR: "BTU/h" },
  status: { ACTIVE: "Ativo", DEACTIVATED: "Desativado" },
}

/** "3 modelos", "1 modelo" ou "Nenhum modelo" (mockups 8 e 10). */
export const countText = (count: number) =>
  count === 0 ? "Nenhum modelo" : `${count} ${count === 1 ? "modelo" : "modelos"}`
