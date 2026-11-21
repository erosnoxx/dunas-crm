import {
  keepPreviousData,
  useMutation,
  useQuery,
  useQueryClient,
  type QueryClient,
} from "@tanstack/react-query"

import * as service from "./catalog.service"

// Chaves: `["catalog", "<recurso>"]` cobre a lista e, no modelo, o detalhe.
const keys = {
  manufacturers: ["catalog", "manufacturers"] as const,
  types: ["catalog", "types"] as const,
  models: ["catalog", "models"] as const,
}

type ListManufacturersInput = Parameters<typeof service.listManufacturers>[0]
type ListTypesInput = Parameters<typeof service.listEquipmentTypes>[0]
type ListModelsInput = Parameters<typeof service.listEquipmentModels>[0]

/** Trocar de página ou de filtro mantém a lista anterior na tela até a nova chegar. */
export function useManufacturers(input: ListManufacturersInput) {
  return useQuery({
    queryKey: [...keys.manufacturers, "list", input],
    queryFn: () => service.listManufacturers(input),
    placeholderData: keepPreviousData,
  })
}

export function useEquipmentTypes(input: ListTypesInput) {
  return useQuery({
    queryKey: [...keys.types, "list", input],
    queryFn: () => service.listEquipmentTypes(input),
    placeholderData: keepPreviousData,
  })
}

export function useEquipmentModels(input: ListModelsInput) {
  return useQuery({
    queryKey: [...keys.models, "list", input],
    queryFn: () => service.listEquipmentModels(input),
    placeholderData: keepPreviousData,
  })
}

export function useEquipmentModel(id: string) {
  return useQuery({
    queryKey: [...keys.models, "detail", id],
    queryFn: () => service.getEquipmentModel(id),
  })
}

const invalidate = (client: QueryClient, ...roots: (typeof keys)[keyof typeof keys][]) =>
  Promise.all(roots.map((queryKey) => client.invalidateQueries({ queryKey })))

// O fabricante e o tipo carregam a contagem de modelos, e o modelo mostra o nome deles:
// por isso mudar um deles invalida também os modelos, e mudar um modelo, os dois.
const { manufacturers, types, models } = keys

export function useRegisterManufacturer() {
  const client = useQueryClient()

  return useMutation({
    mutationFn: service.registerManufacturer,
    onSuccess: () => invalidate(client, manufacturers),
  })
}

export function useRenameManufacturer() {
  const client = useQueryClient()

  return useMutation({
    mutationFn: ({ id, input }: { id: string; input: { name: string } }) =>
      service.renameManufacturer(id, input),
    onSuccess: () => invalidate(client, manufacturers, models),
  })
}

export function useDeactivateManufacturer() {
  const client = useQueryClient()

  return useMutation({
    mutationFn: service.deactivateManufacturer,
    onSuccess: () => invalidate(client, manufacturers, models),
  })
}

export function useReactivateManufacturer() {
  const client = useQueryClient()

  return useMutation({
    mutationFn: service.reactivateManufacturer,
    onSuccess: () => invalidate(client, manufacturers, models),
  })
}

export function useRegisterEquipmentType() {
  const client = useQueryClient()

  return useMutation({
    mutationFn: service.registerEquipmentType,
    onSuccess: () => invalidate(client, types),
  })
}

export function useUpdateEquipmentType() {
  const client = useQueryClient()

  return useMutation({
    mutationFn: ({ id, input }: { id: string; input: service.EquipmentTypeInput }) =>
      service.updateEquipmentType(id, input),
    onSuccess: () => invalidate(client, types, models),
  })
}

export function useDeactivateEquipmentType() {
  const client = useQueryClient()

  return useMutation({
    mutationFn: service.deactivateEquipmentType,
    onSuccess: () => invalidate(client, types, models),
  })
}

export function useReactivateEquipmentType() {
  const client = useQueryClient()

  return useMutation({
    mutationFn: service.reactivateEquipmentType,
    onSuccess: () => invalidate(client, types, models),
  })
}

export function useRegisterEquipmentModel() {
  const client = useQueryClient()

  return useMutation({
    mutationFn: service.registerEquipmentModel,
    onSuccess: () => invalidate(client, models, manufacturers, types),
  })
}

export function useUpdateEquipmentModel() {
  const client = useQueryClient()

  return useMutation({
    mutationFn: ({
      id,
      input,
    }: {
      id: string
      input: Parameters<typeof service.updateEquipmentModel>[1]
    }) => service.updateEquipmentModel(id, input),
    onSuccess: () => invalidate(client, models, manufacturers, types),
  })
}

export function useDeactivateEquipmentModel() {
  const client = useQueryClient()

  return useMutation({
    mutationFn: service.deactivateEquipmentModel,
    onSuccess: () => invalidate(client, models, manufacturers, types),
  })
}

export function useReactivateEquipmentModel() {
  const client = useQueryClient()

  return useMutation({
    mutationFn: service.reactivateEquipmentModel,
    onSuccess: () => invalidate(client, models, manufacturers, types),
  })
}
