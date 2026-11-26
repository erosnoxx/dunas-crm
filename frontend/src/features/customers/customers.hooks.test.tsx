import { QueryClientProvider } from "@tanstack/react-query"
import { renderHook, waitFor } from "@testing-library/react"
import type { ReactNode } from "react"
import { beforeEach, describe, expect, it, vi } from "vitest"

import { createQueryClient } from "@/shared/lib/query-client"

import {
  useCustomer,
  useCustomers,
  useCustomersInfinite,
  useDeactivatedCount,
  useDeactivateLocation,
  useInstallEquipment,
  useModelOptions,
  useRegisterCustomer,
  useUpdateCustomer,
} from "./customers.hooks"
import * as service from "./customers.service"

vi.mock("./customers.service")

function setup() {
  const client = createQueryClient(() => undefined)
  const wrapper = ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={client}>{children}</QueryClientProvider>
  )

  return { client, wrapper }
}

const page = (totalElements: number, current = 0, totalPages = 1) => ({
  content: [],
  page: current,
  size: 20,
  totalElements,
  totalPages,
})

beforeEach(() => vi.resetAllMocks())

describe("leituras", () => {
  it("useCustomers guarda a página sob ['customers', 'list', input]", async () => {
    vi.mocked(service.listCustomers).mockResolvedValue(page(0))
    const input = { text: "ana", status: "ALL", page: 0, size: 20 } as const
    const { client, wrapper } = setup()

    const { result } = renderHook(() => useCustomers(input), { wrapper })

    await waitFor(() => expect(result.current.data).toEqual(page(0)))
    expect(service.listCustomers).toHaveBeenCalledWith(input)
    expect(client.getQueryData(["customers", "list", input])).toEqual(page(0))
  })

  it("useCustomersInfinite pede a página seguinte enquanto houver", async () => {
    vi.mocked(service.listCustomers)
      .mockResolvedValueOnce(page(25, 0, 2))
      .mockResolvedValueOnce(page(25, 1, 2))
    const { wrapper } = setup()

    const { result } = renderHook(() => useCustomersInfinite({ size: 20 }), { wrapper })

    await waitFor(() => expect(result.current.hasNextPage).toBe(true))
    await result.current.fetchNextPage()
    await waitFor(() => expect(result.current.hasNextPage).toBe(false))
    expect(service.listCustomers).toHaveBeenLastCalledWith({ size: 20, page: 1 })
  })

  it("useCustomer guarda a ficha sob ['customers', 'detail', id]", async () => {
    const detail = { id: "c1" } as service.CustomerDetail
    vi.mocked(service.getCustomer).mockResolvedValue(detail)
    const { client, wrapper } = setup()

    const { result } = renderHook(() => useCustomer("c1"), { wrapper })

    await waitFor(() => expect(result.current.data).toEqual(detail))
    expect(client.getQueryData(["customers", "detail", "c1"])).toEqual(detail)
  })

  it("useModelOptions guarda sob ['customers', 'model-options']", async () => {
    vi.mocked(service.listModelOptions).mockResolvedValue([])
    const { client, wrapper } = setup()

    const { result } = renderHook(() => useModelOptions(), { wrapper })

    await waitFor(() => expect(result.current.isSuccess).toBe(true))
    expect(client.getQueryData(["customers", "model-options"])).toEqual([])
  })
})

describe("useDeactivatedCount", () => {
  it("desligado, não consulta", async () => {
    const { wrapper } = setup()

    const { result } = renderHook(() => useDeactivatedCount(false), { wrapper })

    await new Promise((resolve) => setTimeout(resolve, 20))
    expect(service.listCustomers).not.toHaveBeenCalled()
    expect(result.current.data).toBeUndefined()
  })

  it("ligado, pede os desativados com size=1 e devolve o total", async () => {
    vi.mocked(service.listCustomers).mockResolvedValue(page(4))
    const { wrapper } = setup()

    const { result } = renderHook(() => useDeactivatedCount(true), { wrapper })

    await waitFor(() => expect(result.current.data).toBe(4))
    expect(service.listCustomers).toHaveBeenCalledWith({
      status: "DEACTIVATED",
      page: 0,
      size: 1,
    })
  })
})

describe("mutations invalidam ['customers']", () => {
  it("cadastrar cliente", async () => {
    vi.mocked(service.registerCustomer).mockResolvedValue({ id: "c1" })
    const { client, wrapper } = setup()
    const invalidate = vi.spyOn(client, "invalidateQueries")
    const input = {
      document: "12345678909",
      name: "Ana",
      tradeName: null,
      email: "ana@exemplo.com",
      phone: null,
    }

    const { result } = renderHook(() => useRegisterCustomer(), { wrapper })

    expect(await result.current.mutateAsync(input)).toEqual({ id: "c1" })
    expect(service.registerCustomer).toHaveBeenCalledWith(input, expect.anything())
    expect(invalidate).toHaveBeenCalledWith({ queryKey: ["customers"] })
  })

  it("editar cliente passa id e corpo", async () => {
    vi.mocked(service.updateCustomer).mockResolvedValue(undefined)
    const { client, wrapper } = setup()
    const invalidate = vi.spyOn(client, "invalidateQueries")
    const input = { name: "Ana", tradeName: null, email: null, phone: "+5511999999999" }

    const { result } = renderHook(() => useUpdateCustomer(), { wrapper })
    await result.current.mutateAsync({ id: "c1", input })

    expect(service.updateCustomer).toHaveBeenCalledWith("c1", input)
    expect(invalidate).toHaveBeenCalledWith({ queryKey: ["customers"] })
  })

  it("instalar equipamento passa o local e o corpo", async () => {
    vi.mocked(service.installEquipment).mockResolvedValue({ id: "e1" })
    const { client, wrapper } = setup()
    const invalidate = vi.spyOn(client, "invalidateQueries")
    const input = { modelId: "m1", serialNumber: "HV22", installedOn: null }

    const { result } = renderHook(() => useInstallEquipment(), { wrapper })
    await result.current.mutateAsync({ locationId: "l1", input })

    expect(service.installEquipment).toHaveBeenCalledWith("l1", input)
    expect(invalidate).toHaveBeenCalledWith({ queryKey: ["customers"] })
  })

  it("desativar local", async () => {
    vi.mocked(service.deactivateLocation).mockResolvedValue(undefined)
    const { client, wrapper } = setup()
    const invalidate = vi.spyOn(client, "invalidateQueries")

    const { result } = renderHook(() => useDeactivateLocation(), { wrapper })
    await result.current.mutateAsync("l1")

    expect(service.deactivateLocation).toHaveBeenCalledWith("l1", expect.anything())
    expect(invalidate).toHaveBeenCalledWith({ queryKey: ["customers"] })
  })

  it("falha não invalida", async () => {
    vi.mocked(service.deactivateLocation).mockRejectedValue({ status: 422 })
    const { client, wrapper } = setup()
    const invalidate = vi.spyOn(client, "invalidateQueries")

    const { result } = renderHook(() => useDeactivateLocation(), { wrapper })

    await expect(result.current.mutateAsync("l1")).rejects.toEqual({ status: 422 })
    expect(invalidate).not.toHaveBeenCalled()
  })
})
