import { beforeEach, describe, expect, it, vi } from "vitest"

import { ensureCsrfCookie } from "@/shared/api/csrf"
import { httpClient } from "@/shared/api/http-client"

import * as service from "./customers.service"

vi.mock("@/shared/api/http-client", () => ({
  httpClient: { get: vi.fn(), post: vi.fn(), put: vi.fn(), delete: vi.fn() },
}))
vi.mock("@/shared/api/csrf", () => ({ ensureCsrfCookie: vi.fn() }))

const http = vi.mocked(httpClient)
const csrf = vi.mocked(ensureCsrfCookie)
const ID = "7f1c0a52-0000-4000-8000-000000000001"
const EMPTY_PAGE = { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 }

beforeEach(() => {
  vi.resetAllMocks()
  for (const verb of ["get", "post", "put", "delete"] as const)
    http[verb].mockResolvedValue({ data: undefined })
})

describe("leituras", () => {
  it("listCustomers: GET /customers com os filtros como params, sem CSRF", async () => {
    http.get.mockResolvedValue({ data: EMPTY_PAGE })
    const input = { text: "hv22", status: "ALL", page: 0, size: 20 } as const

    expect(await service.listCustomers(input)).toBe(EMPTY_PAGE)
    expect(http.get).toHaveBeenCalledWith("/customers", { params: input })
    expect(csrf).not.toHaveBeenCalled()
  })

  it("getCustomer: GET /customers/{id} e devolve o corpo", async () => {
    http.get.mockResolvedValue({ data: { id: ID } })

    expect(await service.getCustomer(ID)).toEqual({ id: ID })
    expect(http.get).toHaveBeenCalledWith(`/customers/${ID}`)
  })

  it("listModelOptions: os 100 primeiros modelos ativos do catálogo", async () => {
    const model = { id: ID, name: "SUN2000" }
    http.get.mockResolvedValue({ data: { ...EMPTY_PAGE, content: [model] } })

    expect(await service.listModelOptions()).toEqual([model])
    expect(http.get).toHaveBeenCalledWith("/catalog/equipment-models", {
      params: { status: "ACTIVE", page: 0, size: 100 },
    })
  })
})

describe("cliente", () => {
  it("registerCustomer: CSRF, POST /customers com os opcionais null, devolve o id", async () => {
    http.post.mockResolvedValue({ data: { id: ID } })
    const input = {
      document: "12345678909",
      name: "Ana Souza",
      tradeName: null,
      email: null,
      phone: "+5511999999999",
    }

    expect(await service.registerCustomer(input)).toEqual({ id: ID })
    expect(csrf).toHaveBeenCalledBefore(http.post)
    expect(http.post).toHaveBeenCalledWith("/customers", input)
  })

  it("updateCustomer: CSRF e PUT /customers/{id}, sem documento", async () => {
    const input = {
      name: "Padaria LTDA",
      tradeName: "Padaria Boa Vista",
      email: "contato@padaria.com",
      phone: null,
    }

    await service.updateCustomer(ID, input)

    expect(csrf).toHaveBeenCalledBefore(http.put)
    expect(http.put).toHaveBeenCalledWith(`/customers/${ID}`, input)
  })

  it("deactivate e reactivate: CSRF e POST sem corpo", async () => {
    await service.deactivateCustomer(ID)
    await service.reactivateCustomer(ID)

    expect(csrf).toHaveBeenCalledTimes(2)
    expect(http.post).toHaveBeenNthCalledWith(1, `/customers/${ID}/deactivation`)
    expect(http.post).toHaveBeenNthCalledWith(2, `/customers/${ID}/reactivation`)
  })
})

describe("local", () => {
  const input = {
    name: "Matriz",
    address: {
      zipCode: "01310100",
      street: "Avenida Paulista",
      number: "1000",
      complement: null,
      neighborhood: "Bela Vista",
      city: "São Paulo",
      state: "SP",
    },
  } as const

  it("registerLocation: CSRF, POST /customers/{id}/locations e devolve o id", async () => {
    http.post.mockResolvedValue({ data: { id: ID } })

    expect(await service.registerLocation(ID, input)).toEqual({ id: ID })
    expect(csrf).toHaveBeenCalledBefore(http.post)
    expect(http.post).toHaveBeenCalledWith(`/customers/${ID}/locations`, input)
  })

  it("updateLocation: CSRF e PUT /locations/{id}", async () => {
    await service.updateLocation(ID, input)

    expect(csrf).toHaveBeenCalledBefore(http.put)
    expect(http.put).toHaveBeenCalledWith(`/locations/${ID}`, input)
  })

  it("deactivate e reactivate: CSRF e POST sem corpo", async () => {
    await service.deactivateLocation(ID)
    await service.reactivateLocation(ID)

    expect(csrf).toHaveBeenCalledTimes(2)
    expect(http.post).toHaveBeenNthCalledWith(1, `/locations/${ID}/deactivation`)
    expect(http.post).toHaveBeenNthCalledWith(2, `/locations/${ID}/reactivation`)
  })
})

describe("equipamento", () => {
  it("installEquipment: CSRF, POST /locations/{id}/equipment e devolve o id", async () => {
    http.post.mockResolvedValue({ data: { id: ID } })
    const input = { modelId: ID, serialNumber: "HV2211", installedOn: null }

    expect(await service.installEquipment(ID, input)).toEqual({ id: ID })
    expect(csrf).toHaveBeenCalledBefore(http.post)
    expect(http.post).toHaveBeenCalledWith(`/locations/${ID}/equipment`, input)
  })

  it("updateInstalledEquipment: CSRF e PUT /installed-equipment/{id}", async () => {
    const input = { serialNumber: "HV2212", installedOn: "2025-03-12" }

    await service.updateInstalledEquipment(ID, input)

    expect(csrf).toHaveBeenCalledBefore(http.put)
    expect(http.put).toHaveBeenCalledWith(`/installed-equipment/${ID}`, input)
  })

  it("deactivate e reactivate: CSRF e POST sem corpo", async () => {
    await service.deactivateInstalledEquipment(ID)
    await service.reactivateInstalledEquipment(ID)

    expect(csrf).toHaveBeenCalledTimes(2)
    expect(http.post).toHaveBeenNthCalledWith(
      1,
      `/installed-equipment/${ID}/deactivation`,
    )
    expect(http.post).toHaveBeenNthCalledWith(
      2,
      `/installed-equipment/${ID}/reactivation`,
    )
  })
})
