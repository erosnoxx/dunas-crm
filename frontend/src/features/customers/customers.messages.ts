import { UNKNOWN_ERROR_MESSAGE, type ApiError } from "@/shared/api/api-error"

import type { CustomerStatus, Unit } from "./customers.service"

/** Mensagens da tabela de erros de docs/system-design/03-customers/usecases.md, por `code`. */
const MESSAGES: Record<string, string> = {
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

export function messageFor(code: string | undefined, fallback: string): string {
  return (code !== undefined && MESSAGES[code]) || fallback
}

/** O texto da API (minúsculo, sem ponto) como frase da tela: inicial maiúscula e ponto final (ADR-31). */
export function sentence(text: string): string {
  if (text === "") return ""

  const capitalized = text.charAt(0).toUpperCase() + text.slice(1)

  return capitalized.endsWith(".") ? capitalized : `${capitalized}.`
}

/** A mensagem de um erro da API para o aviso: pelo `code`, senão a da própria API, como frase. */
export const errorMessage = (error: Partial<ApiError>) =>
  sentence(messageFor(error.code, error.message || UNKNOWN_ERROR_MESSAGE))

/** A tabela "Mensagens da tela" de docs/system-design/03-customers/usecases.md (ADR-31). */
export const FORM_MESSAGES = {
  documentRequired: "Informe o CPF ou o CNPJ.",
  documentIncomplete:
    "Documento incompleto: o CPF tem 11 dígitos e o CNPJ, 14 caracteres.",
  cpfInvalid: "CPF inválido: confira os números digitados.",
  cnpjInvalid: "CNPJ inválido: confira os caracteres digitados.",
  nameRequired: "Informe o nome.",
  corporateNameRequired: "Informe a razão social.",
  nameLength: "O nome precisa ter de 2 a 150 caracteres.",
  emailInvalid: "E-mail inválido: confira o endereço.",
  phoneInvalid: "Telefone inválido: informe DDD e número.",
  contactRequired: "Informe pelo menos um contato: e-mail ou telefone.",
  locationNameRequired: "Informe o nome do local.",
  locationNameLength: "O nome do local precisa ter de 2 a 60 caracteres.",
  zipCodeRequired: "Informe o CEP.",
  zipCodeIncomplete: "CEP incompleto: informe os 8 dígitos.",
  streetRequired: "Informe o logradouro.",
  streetLength: "O logradouro precisa ter de 2 a 150 caracteres.",
  numberRequired: "Informe o número (sem número, use S/N).",
  neighborhoodRequired: "Informe o bairro.",
  neighborhoodLength: "O bairro precisa ter de 2 a 100 caracteres.",
  cityRequired: "Informe a cidade.",
  stateRequired: "Escolha a UF.",
  modelRequired: "Escolha o modelo.",
  serialNumberRequired: "Informe o número de série.",
  installedOnFuture: "A data de instalação não pode ser depois de hoje.",
} as const

export const LABELS: {
  status: Record<CustomerStatus, string>
  unit: Record<Unit, string>
} = {
  status: { ACTIVE: "Ativo", DEACTIVATED: "Desativado" },
  unit: { KILOWATT: "kW", WATT: "W", WATT_PEAK: "Wp", BTU_PER_HOUR: "BTU/h" },
}

const plural = (count: number, one: string, many: string) =>
  `${count} ${count === 1 ? one : many}`

/** "2 ativos, 1 desativado"; sem desativado, só "1 ativo" (telas 9 e 12). */
export function locationsSummary(active: number, deactivated: number): string {
  const activeText = plural(active, "ativo", "ativos")

  if (deactivated === 0) return activeText

  return `${activeText}, ${plural(deactivated, "desativado", "desativados")}`
}

/** "3 equipamentos ativos", "1 equipamento ativo". */
export const activeEquipmentText = (count: number) =>
  plural(count, "equipamento ativo", "equipamentos ativos")

/** "2 locais · 5 equip.", "1 local · 3 equip." (lista no celular). */
export const listCounts = (locations: number, equipment: number) =>
  `${plural(locations, "local", "locais")} · ${equipment} equip.`
