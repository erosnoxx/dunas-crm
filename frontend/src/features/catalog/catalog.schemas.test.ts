import { describe, expect, it } from "vitest"

import {
  equipmentModelSchema,
  equipmentTypeSchema,
  manufacturerSchema,
} from "./catalog.schemas"

const ID = "7f1c0a52-0000-4000-8000-000000000001"
const text = (n: number) => "a".repeat(n)

const type = {
  name: "Inversor",
  characteristicName: "Potência",
  unit: "KILOWATT",
  required: true,
}
const model = { manufacturerId: ID, typeId: ID, name: "SUN2000", amount: "5" }

describe("manufacturerSchema", () => {
  it("aceita de 2 a 80 caracteres e apara as pontas", () => {
    expect(manufacturerSchema.safeParse({ name: text(2) }).success).toBe(true)
    expect(manufacturerSchema.safeParse({ name: text(80) }).success).toBe(true)
    expect(manufacturerSchema.parse({ name: "  Huawei  " }).name).toBe("Huawei")
  })

  it("recusa 1 e 81 caracteres, e só espaços", () => {
    expect(manufacturerSchema.safeParse({ name: text(1) }).success).toBe(false)
    expect(manufacturerSchema.safeParse({ name: text(81) }).success).toBe(false)
    expect(manufacturerSchema.safeParse({ name: "   " }).success).toBe(false)
  })
})

describe("equipmentTypeSchema", () => {
  it("aceita os limites do nome (2 a 60) e da característica (2 a 40)", () => {
    for (const [name, characteristicName] of [
      [text(2), text(2)],
      [text(60), text(40)],
    ])
      expect(
        equipmentTypeSchema.safeParse({ ...type, name, characteristicName }).success,
      ).toBe(true)
  })

  it("recusa nome fora de 2 a 60 e característica fora de 2 a 40", () => {
    for (const patch of [
      { name: text(1) },
      { name: text(61) },
      { characteristicName: text(1) },
      { characteristicName: text(41) },
      { unit: "METRO" },
    ])
      expect(equipmentTypeSchema.safeParse({ ...type, ...patch }).success).toBe(false)
  })
})

describe("equipmentModelSchema", () => {
  it("aceita nome de 1 a 80 e valor válido", () => {
    expect(equipmentModelSchema.safeParse({ ...model, name: text(1) }).success).toBe(true)
    expect(equipmentModelSchema.safeParse({ ...model, name: text(80) }).success).toBe(
      true,
    )
    expect(equipmentModelSchema.safeParse({ ...model, amount: "0,5" }).success).toBe(true)
  })

  it("recusa nome vazio ou com 81, fabricante ou tipo sem escolha e valor inválido", () => {
    for (const patch of [
      { name: "" },
      { name: text(81) },
      { manufacturerId: "" },
      { typeId: "" },
      { amount: "12.000" },
      { amount: "0" },
    ])
      expect(equipmentModelSchema.safeParse({ ...model, ...patch }).success).toBe(false)
  })
})
