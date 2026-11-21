import { QueryClientProvider } from "@tanstack/react-query"
import { renderHook, waitFor } from "@testing-library/react"
import type { ReactNode } from "react"
import { beforeEach, describe, expect, it, vi } from "vitest"

import { createQueryClient } from "@/shared/lib/query-client"

import {
  useDeactivateManufacturer,
  useEquipmentModels,
  useRegisterManufacturer,
  useUpdateEquipmentModel,
  useUpdateEquipmentType,
} from "./catalog.hooks"
import * as service from "./catalog.service"

vi.mock("./catalog.service")

function setup() {
  const client = createQueryClient(() => undefined)
  const wrapper = ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={client}>{children}</QueryClientProvider>
  )

  return { client, wrapper }
}

beforeEach(() => vi.resetAllMocks())

const typeBody = {
  name: "Inversor",
  characteristic: { name: "Potência", unit: "KILOWATT", required: true },
} as const

describe("leituras", () => {
  it("useEquipmentModels guarda o resultado sob ['catalog', 'models', ...]", async () => {
    const page = { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 }
    vi.mocked(service.listEquipmentModels).mockResolvedValue(page)
    const input = { page: 0, size: 20 }
    const { client, wrapper } = setup()

    const { result } = renderHook(() => useEquipmentModels(input), { wrapper })

    await waitFor(() => expect(result.current.data).toEqual(page))
    expect(client.getQueryData(["catalog", "models", "list", input])).toEqual(page)
  })
})

describe("mutations", () => {
  it("mudar o tipo invalida tipos e modelos", async () => {
    vi.mocked(service.updateEquipmentType).mockResolvedValue(undefined)
    const { client, wrapper } = setup()
    const invalidate = vi.spyOn(client, "invalidateQueries")

    const { result } = renderHook(() => useUpdateEquipmentType(), { wrapper })
    await result.current.mutateAsync({ id: "t1", input: typeBody })

    expect(service.updateEquipmentType).toHaveBeenCalledWith("t1", typeBody)
    expect(invalidate).toHaveBeenCalledWith({ queryKey: ["catalog", "types"] })
    expect(invalidate).toHaveBeenCalledWith({ queryKey: ["catalog", "models"] })
  })

  it("mudar o fabricante invalida fabricantes e modelos", async () => {
    vi.mocked(service.deactivateManufacturer).mockResolvedValue(undefined)
    const { client, wrapper } = setup()
    const invalidate = vi.spyOn(client, "invalidateQueries")

    const { result } = renderHook(() => useDeactivateManufacturer(), { wrapper })
    await result.current.mutateAsync("m1")

    expect(invalidate).toHaveBeenCalledWith({ queryKey: ["catalog", "manufacturers"] })
    expect(invalidate).toHaveBeenCalledWith({ queryKey: ["catalog", "models"] })
  })

  it("cadastrar fabricante invalida só fabricantes (modelo novo não muda)", async () => {
    vi.mocked(service.registerManufacturer).mockResolvedValue({ id: "m1" })
    const { client, wrapper } = setup()
    const invalidate = vi.spyOn(client, "invalidateQueries")

    const { result } = renderHook(() => useRegisterManufacturer(), { wrapper })
    await result.current.mutateAsync({ name: "Huawei" })

    expect(invalidate).toHaveBeenCalledWith({ queryKey: ["catalog", "manufacturers"] })
    expect(invalidate).not.toHaveBeenCalledWith({ queryKey: ["catalog", "models"] })
  })

  it("mudar o modelo invalida modelos, fabricantes e tipos (contagem de modelos)", async () => {
    vi.mocked(service.updateEquipmentModel).mockResolvedValue(undefined)
    const { client, wrapper } = setup()
    const invalidate = vi.spyOn(client, "invalidateQueries")

    const { result } = renderHook(() => useUpdateEquipmentModel(), { wrapper })
    await result.current.mutateAsync({ id: "e1", input: { name: "X", values: [] } })

    for (const key of ["models", "manufacturers", "types"])
      expect(invalidate).toHaveBeenCalledWith({ queryKey: ["catalog", key] })
  })
})
