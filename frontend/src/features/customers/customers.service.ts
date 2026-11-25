import { ensureCsrfCookie } from "@/shared/api/csrf"
import { httpClient } from "@/shared/api/http-client"
import type { Page } from "@/shared/api/page"

export type CustomerStatus = "ACTIVE" | "DEACTIVATED"
export type StatusFilter = CustomerStatus | "ALL"
export type DocumentType = "CPF" | "CNPJ"
export type Unit = "KILOWATT" | "WATT" | "WATT_PEAK" | "BTU_PER_HOUR"

// prettier-ignore
export const UFS = [
  "AC", "AL", "AP", "AM", "BA", "CE", "DF", "ES", "GO", "MA", "MT", "MS", "MG", "PA",
  "PB", "PR", "PE", "PI", "RJ", "RN", "RS", "RO", "RR", "SC", "SP", "SE", "TO",
] as const

export type UF = (typeof UFS)[number]

export type CustomerSummary = {
  id: string
  name: string
  tradeName: string | null
  /** Normalizado, como o `Document` guarda; a tela formata (`formatDocument`). */
  document: string
  documentType: DocumentType
  status: CustomerStatus
  locationCount: number
  equipmentCount: number
  cities: { city: string; state: UF }[]
  matchedSerials: { serialNumber: string; modelName: string }[]
}

export type Address = {
  zipCode: string
  street: string
  number: string
  complement: string | null
  neighborhood: string
  city: string
  state: UF
}

export type EquipmentView = {
  id: string
  serialNumber: string
  installedOn: string | null
  status: CustomerStatus
  model: { id: string; name: string; status: CustomerStatus }
  manufacturer: { id: string; name: string }
  type: { id: string; name: string }
  /** O `amount` chega como texto ou número JSON; a tela o converte (`formatAmount`). */
  characteristic: { name: string; unit: Unit; amount: string | number } | null
}

export type LocationView = {
  id: string
  name: string
  status: CustomerStatus
  address: Address
  equipment: EquipmentView[]
}

export type CustomerDetail = {
  id: string
  document: string
  documentType: DocumentType
  name: string
  tradeName: string | null
  email: string | null
  phone: string | null
  status: CustomerStatus
  locations: LocationView[]
}

/** O recorte do modelo do catálogo que o formulário de equipamento usa (ADR-8). */
export type ModelOption = {
  id: string
  name: string
  manufacturer: { id: string; name: string }
  type: { id: string; name: string }
  values: { name: string; unit: Unit; amount: string | number }[]
}

/** Opcional vazio vai como `null` (ADR-31); telefone em E.164 (ADR-18). */
export type CustomerInput = {
  name: string
  tradeName: string | null
  email: string | null
  phone: string | null
}

export type LocationInput = { name: string; address: Address }

export type EquipmentInput = {
  modelId: string
  serialNumber: string
  installedOn: string | null
}

const CUSTOMERS = "/customers"
const LOCATIONS = "/locations"
const EQUIPMENT = "/installed-equipment"
const MODELS = "/catalog/equipment-models"

async function create(url: string, body: object): Promise<{ id: string }> {
  await ensureCsrfCookie()
  const { data } = await httpClient.post<{ id: string }>(url, body)

  return data
}

async function replace(url: string, body: object): Promise<void> {
  await ensureCsrfCookie()
  await httpClient.put(url, body)
}

async function act(url: string): Promise<void> {
  await ensureCsrfCookie()
  await httpClient.post(url)
}

export async function listCustomers(input: {
  text?: string
  status?: StatusFilter
  page: number
  size: number
}): Promise<Page<CustomerSummary>> {
  const { data } = await httpClient.get<Page<CustomerSummary>>(CUSTOMERS, {
    params: input,
  })

  return data
}

export async function getCustomer(id: string): Promise<CustomerDetail> {
  const { data } = await httpClient.get<CustomerDetail>(`${CUSTOMERS}/${id}`)

  return data
}

export const registerCustomer = (input: CustomerInput & { document: string }) =>
  create(CUSTOMERS, input)

export const updateCustomer = (id: string, input: CustomerInput) =>
  replace(`${CUSTOMERS}/${id}`, input)

export const deactivateCustomer = (id: string) => act(`${CUSTOMERS}/${id}/deactivation`)

export const reactivateCustomer = (id: string) => act(`${CUSTOMERS}/${id}/reactivation`)

export const registerLocation = (customerId: string, input: LocationInput) =>
  create(`${CUSTOMERS}/${customerId}/locations`, input)

export const updateLocation = (id: string, input: LocationInput) =>
  replace(`${LOCATIONS}/${id}`, input)

export const deactivateLocation = (id: string) => act(`${LOCATIONS}/${id}/deactivation`)

export const reactivateLocation = (id: string) => act(`${LOCATIONS}/${id}/reactivation`)

export const installEquipment = (locationId: string, input: EquipmentInput) =>
  create(`${LOCATIONS}/${locationId}/equipment`, input)

export const updateInstalledEquipment = (
  id: string,
  input: Omit<EquipmentInput, "modelId">,
) => replace(`${EQUIPMENT}/${id}`, input)

export const deactivateInstalledEquipment = (id: string) =>
  act(`${EQUIPMENT}/${id}/deactivation`)

export const reactivateInstalledEquipment = (id: string) =>
  act(`${EQUIPMENT}/${id}/reactivation`)

// shortcut: a escolha do modelo lista só os 100 primeiros ativos; paginar a escolha se o catálogo passar disso.
export async function listModelOptions(): Promise<ModelOption[]> {
  const { data } = await httpClient.get<Page<ModelOption>>(MODELS, {
    params: { status: "ACTIVE", page: 0, size: 100 },
  })

  return data.content
}
