import { z } from "zod"

import { parseAmount } from "./amount"

// Limites de docs/system-design/02-catalog/domain-model.md. Aparar as pontas é seguro: nome não é senha.
const name = (label: string, min: number, max: number) =>
  z
    .string()
    .trim()
    .min(min, min === 1 ? `informe ${label}` : `${label} tem no mínimo ${min} caracteres`)
    .max(max, `${label} tem no máximo ${max} caracteres`)

export const manufacturerSchema = z.object({ name: name("o nome", 2, 80) })

export const equipmentTypeSchema = z.object({
  name: name("o nome", 2, 60),
  characteristicName: name("o nome da característica", 2, 40),
  unit: z.enum(["KILOWATT", "WATT", "WATT_PEAK", "BTU_PER_HOUR"], {
    error: "escolha a unidade",
  }),
  required: z.boolean(),
})

const amount = (optional: boolean) =>
  z.string().superRefine((value, context) => {
    if (optional && value === "") return

    const parsed = parseAmount(value)

    if (!parsed.ok) context.addIssue({ code: "custom", message: parsed.reason })
  })

export const equipmentModelSchema = z.object({
  manufacturerId: z.string().min(1, "escolha o fabricante"),
  typeId: z.string().min(1, "escolha o tipo"),
  name: name("o nome", 1, 80),
  amount: amount(false),
})

/** O mesmo, para o tipo cuja característica não é obrigatória: o valor vazio vale. */
export const optionalAmountModelSchema = equipmentModelSchema.extend({
  amount: amount(true),
})

export type ManufacturerValues = z.infer<typeof manufacturerSchema>
export type EquipmentTypeValues = z.infer<typeof equipmentTypeSchema>
export type EquipmentModelValues = z.infer<typeof equipmentModelSchema>
