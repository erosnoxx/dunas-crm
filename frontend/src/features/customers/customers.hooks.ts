import {
  keepPreviousData,
  useInfiniteQuery,
  useMutation,
  useQuery,
  useQueryClient,
} from "@tanstack/react-query"

import * as service from "./customers.service"

// Tudo mora sob `["customers"]`: a ficha traz locais e equipamentos e a lista traz as contagens,
// então qualquer escrita invalida a raiz inteira.
const ROOT = ["customers"] as const

type ListInput = Parameters<typeof service.listCustomers>[0]

/** Trocar de página ou de filtro mantém a lista anterior na tela até a nova chegar. */
export function useCustomers(input: ListInput) {
  return useQuery({
    queryKey: [...ROOT, "list", input],
    queryFn: () => service.listCustomers(input),
    placeholderData: keepPreviousData,
  })
}

/** Páginas acumuladas (celular, "Carregar mais"). */
export function useCustomersInfinite(input: Omit<ListInput, "page">) {
  return useInfiniteQuery({
    queryKey: [...ROOT, "infinite", input],
    queryFn: ({ pageParam }) => service.listCustomers({ ...input, page: pageParam }),
    initialPageParam: 0,
    getNextPageParam: (last) => (last.page + 1 < last.totalPages ? last.page + 1 : null),
    placeholderData: keepPreviousData,
  })
}

/** Quantos desativados há: só ligado quando a lista padrão volta vazia (ADR-28). */
export function useDeactivatedCount(enabled: boolean) {
  const input = { status: "DEACTIVATED", page: 0, size: 1 } as const

  return useQuery({
    queryKey: [...ROOT, "list", input],
    queryFn: () => service.listCustomers(input),
    select: (page) => page.totalElements,
    enabled,
  })
}

export function useCustomer(id: string) {
  return useQuery({
    queryKey: [...ROOT, "detail", id],
    queryFn: () => service.getCustomer(id),
  })
}

export function useModelOptions() {
  return useQuery({
    queryKey: [...ROOT, "model-options"],
    queryFn: service.listModelOptions,
  })
}

function useWrite<TInput, TResult>(mutationFn: (input: TInput) => Promise<TResult>) {
  const client = useQueryClient()

  return useMutation({
    mutationFn,
    onSuccess: () => client.invalidateQueries({ queryKey: ROOT }),
  })
}

type Update<T> = { id: string; input: T }

export const useRegisterCustomer = () => useWrite(service.registerCustomer)

export const useUpdateCustomer = () =>
  useWrite(({ id, input }: Update<service.CustomerInput>) =>
    service.updateCustomer(id, input),
  )

export const useDeactivateCustomer = () => useWrite(service.deactivateCustomer)

export const useReactivateCustomer = () => useWrite(service.reactivateCustomer)

export const useRegisterLocation = () =>
  useWrite(
    ({ customerId, input }: { customerId: string; input: service.LocationInput }) =>
      service.registerLocation(customerId, input),
  )

export const useUpdateLocation = () =>
  useWrite(({ id, input }: Update<service.LocationInput>) =>
    service.updateLocation(id, input),
  )

export const useDeactivateLocation = () => useWrite(service.deactivateLocation)

export const useReactivateLocation = () => useWrite(service.reactivateLocation)

export const useInstallEquipment = () =>
  useWrite(
    ({ locationId, input }: { locationId: string; input: service.EquipmentInput }) =>
      service.installEquipment(locationId, input),
  )

export const useUpdateInstalledEquipment = () =>
  useWrite(({ id, input }: Update<Omit<service.EquipmentInput, "modelId">>) =>
    service.updateInstalledEquipment(id, input),
  )

export const useDeactivateInstalledEquipment = () =>
  useWrite(service.deactivateInstalledEquipment)

export const useReactivateInstalledEquipment = () =>
  useWrite(service.reactivateInstalledEquipment)
