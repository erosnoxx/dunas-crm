import { describe, expect, it } from "vitest"

import { formatAmount, formatDate, orderCities } from "./display"
import type { UF } from "./customers.service"

describe("formatDate", () => {
  it("a data da API vira dia/mês/ano", () => {
    expect(formatDate("2025-03-12")).toBe("12/03/2025")
    expect(formatDate("2024-12-01")).toBe("01/12/2024")
  })
})

describe("formatAmount", () => {
  it.each([
    ["12000", "12000"],
    [12000, "12000"],
    ["12000.000", "12000"],
    ["5.5", "5,5"],
    [0.5, "0,5"],
    ["3", "3"],
  ])("%j vira %j, sem milhar", (value, text) => {
    expect(formatAmount(value)).toBe(text)
  })
})

describe("orderCities", () => {
  const city = (name: string, state: UF = "MG") => ({ city: name, state })
  const cities = [city("Araguari"), city("Belo Horizonte"), city("Uberlândia")]

  it("a cidade que casa a busca vem primeiro, sem diferenciar maiúsculas", () => {
    expect(orderCities(cities, "uberl").map((c) => c.city)).toEqual([
      "Uberlândia",
      "Araguari",
      "Belo Horizonte",
    ])
  })

  it("sem busca, ou sem cidade que case, fica por nome", () => {
    const shuffled = [city("Uberlândia"), city("Araguari"), city("Belo Horizonte")]

    expect(orderCities(shuffled, "").map((c) => c.city)).toEqual([
      "Araguari",
      "Belo Horizonte",
      "Uberlândia",
    ])
    expect(orderCities(shuffled, "hv22").map((c) => c.city)).toEqual([
      "Araguari",
      "Belo Horizonte",
      "Uberlândia",
    ])
  })

  it("não muda a lista recebida", () => {
    const original = [city("Uberlândia"), city("Araguari")]

    orderCities(original, "")

    expect(original.map((c) => c.city)).toEqual(["Uberlândia", "Araguari"])
  })
})
