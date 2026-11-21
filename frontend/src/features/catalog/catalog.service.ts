import { ensureCsrfCookie } from "@/shared/api/csrf"
import { httpClient } from "@/shared/api/http-client"
import type { Page } from "@/shared/api/page"

export type CatalogStatus = "ACTIVE" | "DEACTIVATED"
export type StatusFilter = CatalogStatus | "ALL"
export type Unit = "KILOWATT" | "WATT" | "WATT_PEAK" | "BTU_PER_HOUR"

export type ManufacturerSummary = {
  id: string
  name: string
  status: CatalogStatus
  modelCount: number
}

export type EquipmentTypeSummary = {
  id: string
  name: string
  status: CatalogStatus
  modelCount: number
  characteristics: { id: string; name: string; unit: Unit; required: boolean }[]
}

export type EquipmentModelSummary = {
  id: string
  name: string
  status: CatalogStatus
  manufacturer: { id: string; name: string }
  type: { id: string; name: string }
  /** O `amount` chega como texto ou número JSON; a tela o converte (`formatAmount`). */
  values: { definitionId: string; name: string; unit: Unit; amount: string | number }[]
}

export type EquipmentTypeInput = {
  name: string
  characteristic: { name: string; unit: Unit; required: boolean }
}

export type ModelValuesInput = { definitionId: string; amount: string }[]

const MANUFACTURERS = "/catalog/manufacturers"
const TYPES = "/catalog/equipment-types"
const MODELS = "/catalog/equipment-models"

async function list<T>(url: string, params: object): Promise<Page<T>> {
  const { data } = await httpClient.get<Page<T>>(url, { params })

  return data
}

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

export function listManufacturers(input: {
  text?: string
  status?: StatusFilter
  page: number
  size: number
}) {
  return list<ManufacturerSummary>(MANUFACTURERS, input)
}

export const registerManufacturer = (input: { name: string }) =>
  create(MANUFACTURERS, input)

export const renameManufacturer = (id: string, input: { name: string }) =>
  replace(`${MANUFACTURERS}/${id}`, input)

export const deactivateManufacturer = (id: string) =>
  act(`${MANUFACTURERS}/${id}/deactivation`)

export const reactivateManufacturer = (id: string) =>
  act(`${MANUFACTURERS}/${id}/reactivation`)

export function listEquipmentTypes(input: {
  status?: StatusFilter
  page: number
  size: number
}) {
  return list<EquipmentTypeSummary>(TYPES, input)
}

export const registerEquipmentType = (input: EquipmentTypeInput) => create(TYPES, input)

export const updateEquipmentType = (id: string, input: EquipmentTypeInput) =>
  replace(`${TYPES}/${id}`, input)

export const deactivateEquipmentType = (id: string) => act(`${TYPES}/${id}/deactivation`)

export const reactivateEquipmentType = (id: string) => act(`${TYPES}/${id}/reactivation`)

export function listEquipmentModels(input: {
  text?: string
  typeId?: string
  status?: StatusFilter
  page: number
  size: number
}) {
  return list<EquipmentModelSummary>(MODELS, input)
}

export async function getEquipmentModel(id: string): Promise<EquipmentModelSummary> {
  const { data } = await httpClient.get<EquipmentModelSummary>(`${MODELS}/${id}`)

  return data
}

export const registerEquipmentModel = (input: {
  manufacturerId: string
  typeId: string
  name: string
  values: ModelValuesInput
}) => create(MODELS, input)

export const updateEquipmentModel = (
  id: string,
  input: { name: string; values: ModelValuesInput },
) => replace(`${MODELS}/${id}`, input)

export const deactivateEquipmentModel = (id: string) =>
  act(`${MODELS}/${id}/deactivation`)

export const reactivateEquipmentModel = (id: string) =>
  act(`${MODELS}/${id}/reactivation`)
