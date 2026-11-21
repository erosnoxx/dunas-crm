import { beforeEach, describe, expect, it, vi } from "vitest"

import { ensureCsrfCookie } from "@/shared/api/csrf"
import { httpClient } from "@/shared/api/http-client"

import * as service from "./catalog.service"

vi.mock("@/shared/api/http-client", () => ({
  httpClient: { get: vi.fn(), post: vi.fn(), put: vi.fn(), delete: vi.fn() },
}))
vi.mock("@/shared/api/csrf", () => ({ ensureCsrfCookie: vi.fn() }))

const http = vi.mocked(httpClient)
const csrf = vi.mocked(ensureCsrfCookie)
const ID = "7f1c0a52-0000-4000-8000-000000000001"

beforeEach(() => {
  vi.resetAllMocks()
  for (const verb of ["get", "post", "put", "delete"] as const)
    http[verb].mockResolvedValue({ data: undefined })
})

describe("leituras", () => {
  it("listManufacturers: GET /catalog/manufacturers com os filtros como params", async () => {
    const page = { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 }
    http.get.mockResolvedValue({ data: page })

    const input = { text: "hua", status: "ALL", page: 0, size: 20 } as const

    expect(await service.listManufacturers(input)).toBe(page)
    expect(http.get).toHaveBeenCalledWith("/catalog/manufacturers", { params: input })
    expect(csrf).not.toHaveBeenCalled()
  })

  it("listEquipmentTypes: GET /catalog/equipment-types", async () => {
    await service.listEquipmentTypes({ status: "ACTIVE", page: 1, size: 50 })

    expect(http.get).toHaveBeenCalledWith("/catalog/equipment-types", {
      params: { status: "ACTIVE", page: 1, size: 50 },
    })
  })

  it("listEquipmentModels: GET /catalog/equipment-models", async () => {
    await service.listEquipmentModels({ text: "sun", typeId: ID, page: 0, size: 20 })

    expect(http.get).toHaveBeenCalledWith("/catalog/equipment-models", {
      params: { text: "sun", typeId: ID, page: 0, size: 20 },
    })
  })

  it("getEquipmentModel: GET /catalog/equipment-models/{id} e devolve o corpo", async () => {
    http.get.mockResolvedValue({ data: { id: ID } })

    expect(await service.getEquipmentModel(ID)).toEqual({ id: ID })
    expect(http.get).toHaveBeenCalledWith(`/catalog/equipment-models/${ID}`)
  })
})

describe("fabricante", () => {
  it("registerManufacturer: CSRF, POST e devolve o id", async () => {
    http.post.mockResolvedValue({ data: { id: ID } })

    expect(await service.registerManufacturer({ name: "Huawei" })).toEqual({ id: ID })
    expect(csrf).toHaveBeenCalledBefore(http.post)
    expect(http.post).toHaveBeenCalledWith("/catalog/manufacturers", { name: "Huawei" })
  })

  it("renameManufacturer: CSRF e PUT /catalog/manufacturers/{id}", async () => {
    await service.renameManufacturer(ID, { name: "Solis" })

    expect(csrf).toHaveBeenCalledBefore(http.put)
    expect(http.put).toHaveBeenCalledWith(`/catalog/manufacturers/${ID}`, {
      name: "Solis",
    })
  })

  it("deactivate e reactivate: CSRF e POST sem corpo", async () => {
    await service.deactivateManufacturer(ID)
    await service.reactivateManufacturer(ID)

    expect(csrf).toHaveBeenCalledTimes(2)
    expect(http.post).toHaveBeenNthCalledWith(
      1,
      `/catalog/manufacturers/${ID}/deactivation`,
    )
    expect(http.post).toHaveBeenNthCalledWith(
      2,
      `/catalog/manufacturers/${ID}/reactivation`,
    )
  })
})

describe("tipo", () => {
  const body = {
    name: "Inversor",
    characteristic: { name: "Potência", unit: "KILOWATT", required: true },
  } as const

  it("registerEquipmentType: CSRF, POST e devolve o id", async () => {
    http.post.mockResolvedValue({ data: { id: ID } })

    expect(await service.registerEquipmentType(body)).toEqual({ id: ID })
    expect(csrf).toHaveBeenCalledBefore(http.post)
    expect(http.post).toHaveBeenCalledWith("/catalog/equipment-types", body)
  })

  it("updateEquipmentType: CSRF e PUT /catalog/equipment-types/{id}", async () => {
    await service.updateEquipmentType(ID, body)

    expect(csrf).toHaveBeenCalledBefore(http.put)
    expect(http.put).toHaveBeenCalledWith(`/catalog/equipment-types/${ID}`, body)
  })

  it("deactivate e reactivate: CSRF e POST sem corpo", async () => {
    await service.deactivateEquipmentType(ID)
    await service.reactivateEquipmentType(ID)

    expect(csrf).toHaveBeenCalledTimes(2)
    expect(http.post).toHaveBeenNthCalledWith(
      1,
      `/catalog/equipment-types/${ID}/deactivation`,
    )
    expect(http.post).toHaveBeenNthCalledWith(
      2,
      `/catalog/equipment-types/${ID}/reactivation`,
    )
  })
})

describe("modelo", () => {
  const values = [{ definitionId: ID, amount: "5.5" }]

  it("registerEquipmentModel: CSRF, POST e devolve o id", async () => {
    http.post.mockResolvedValue({ data: { id: ID } })
    const input = { manufacturerId: ID, typeId: ID, name: "SUN2000", values }

    expect(await service.registerEquipmentModel(input)).toEqual({ id: ID })
    expect(csrf).toHaveBeenCalledBefore(http.post)
    expect(http.post).toHaveBeenCalledWith("/catalog/equipment-models", input)
  })

  it("updateEquipmentModel: CSRF e PUT /catalog/equipment-models/{id}", async () => {
    await service.updateEquipmentModel(ID, { name: "SUN2000", values })

    expect(csrf).toHaveBeenCalledBefore(http.put)
    expect(http.put).toHaveBeenCalledWith(`/catalog/equipment-models/${ID}`, {
      name: "SUN2000",
      values,
    })
  })

  it("deactivate e reactivate: CSRF e POST sem corpo", async () => {
    await service.deactivateEquipmentModel(ID)
    await service.reactivateEquipmentModel(ID)

    expect(csrf).toHaveBeenCalledTimes(2)
    expect(http.post).toHaveBeenNthCalledWith(
      1,
      `/catalog/equipment-models/${ID}/deactivation`,
    )
    expect(http.post).toHaveBeenNthCalledWith(
      2,
      `/catalog/equipment-models/${ID}/reactivation`,
    )
  })
})
