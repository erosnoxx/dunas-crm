import { describe, expect, it } from "vitest"
import type { z } from "zod"

import { FORM_MESSAGES as M } from "./customers.messages"
import { customerSchema, equipmentSchema, locationSchema } from "./customers.schemas"

const TODAY = "2026-11-24"
const ID = "7f1c0a52-0000-4000-8000-000000000001"
const text = (n: number) => "a".repeat(n)

const person = {
  document: "123.456.789-09",
  name: "Ana Souza",
  tradeName: "",
  email: "ana@exemplo.com",
  phone: "",
}
const company = { ...person, document: "12.ABC.345/01DE-35", tradeName: "Padaria" }
const location = {
  name: "Matriz",
  zipCode: "01310-100",
  street: "Avenida Paulista",
  number: "1000",
  complement: "",
  neighborhood: "Bela Vista",
  city: "São Paulo",
  state: "SP",
}
const equipment = { modelId: ID, serialNumber: "HV2211", installedOn: "" }

/** A primeira mensagem no caminho, como o resolver do formulário a mostra; `undefined` se passou. */
function messageAt(schema: z.ZodType, values: object, path: string) {
  const result = schema.safeParse(values)

  return result.error?.issues.find((issue) => issue.path.join(".") === path)?.message
}

// Uma linha por linha da tabela "Mensagens da tela" de docs/system-design/03-customers/usecases.md.
describe("Mensagens da tela: cada linha recusa com o texto da tabela", () => {
  it.each([
    [
      "CPF ou CNPJ vazio",
      customerSchema,
      { ...person, document: " " },
      "document",
      M.documentRequired,
    ],
    [
      "documento com 10 caracteres",
      customerSchema,
      { ...person, document: "123.456.789-0" },
      "document",
      M.documentIncomplete,
    ],
    [
      "documento com 12 caracteres",
      customerSchema,
      { ...person, document: "12.ABC.345/01" },
      "document",
      M.documentIncomplete,
    ],
    [
      "CPF com dígito errado",
      customerSchema,
      { ...person, document: "123.456.789-08" },
      "document",
      M.cpfInvalid,
    ],
    [
      "CNPJ com dígito errado",
      customerSchema,
      { ...company, document: "12.ABC.345/01DE-36" },
      "document",
      M.cnpjInvalid,
    ],
    [
      "nome vazio (CPF)",
      customerSchema,
      { ...person, name: "  " },
      "name",
      M.nameRequired,
    ],
    [
      "razão social vazia (CNPJ)",
      customerSchema,
      { ...company, name: "" },
      "name",
      M.corporateNameRequired,
    ],
    [
      "nome com 1 caractere",
      customerSchema,
      { ...person, name: text(1) },
      "name",
      M.nameLength,
    ],
    [
      "nome com 151 caracteres",
      customerSchema,
      { ...person, name: text(151) },
      "name",
      M.nameLength,
    ],
    [
      "nome fantasia com 1 caractere",
      customerSchema,
      { ...company, tradeName: text(1) },
      "tradeName",
      M.nameLength,
    ],
    [
      "nome fantasia com 151 caracteres",
      customerSchema,
      { ...company, tradeName: text(151) },
      "tradeName",
      M.nameLength,
    ],
    [
      "e-mail sem domínio",
      customerSchema,
      { ...person, email: "ana@exemplo" },
      "email",
      M.emailInvalid,
    ],
    [
      "e-mail sem arroba",
      customerSchema,
      { ...person, email: "ana.exemplo.com" },
      "email",
      M.emailInvalid,
    ],
    [
      "telefone sem DDD válido",
      customerSchema,
      { ...person, phone: "(01) 3212-4580" },
      "phone",
      M.phoneInvalid,
    ],
    [
      "telefone com 7 dígitos",
      customerSchema,
      { ...person, phone: "(34) 321-4580" },
      "phone",
      M.phoneInvalid,
    ],
    [
      "celular sem o 9 inicial",
      customerSchema,
      { ...person, phone: "(11) 89999-9999" },
      "phone",
      M.phoneInvalid,
    ],
    [
      "e-mail e telefone vazios",
      customerSchema,
      { ...person, email: " ", phone: "" },
      "contact",
      M.contactRequired,
    ],
    [
      "nome do local vazio",
      locationSchema,
      { ...location, name: " " },
      "name",
      M.locationNameRequired,
    ],
    [
      "nome do local com 1 caractere",
      locationSchema,
      { ...location, name: text(1) },
      "name",
      M.locationNameLength,
    ],
    [
      "nome do local com 61 caracteres",
      locationSchema,
      { ...location, name: text(61) },
      "name",
      M.locationNameLength,
    ],
    [
      "CEP vazio",
      locationSchema,
      { ...location, zipCode: "" },
      "zipCode",
      M.zipCodeRequired,
    ],
    [
      "CEP com 7 dígitos",
      locationSchema,
      { ...location, zipCode: "01310-10" },
      "zipCode",
      M.zipCodeIncomplete,
    ],
    [
      "logradouro vazio",
      locationSchema,
      { ...location, street: "" },
      "street",
      M.streetRequired,
    ],
    [
      "logradouro com 1 caractere",
      locationSchema,
      { ...location, street: text(1) },
      "street",
      M.streetLength,
    ],
    [
      "logradouro com 151 caracteres",
      locationSchema,
      { ...location, street: text(151) },
      "street",
      M.streetLength,
    ],
    [
      "número vazio",
      locationSchema,
      { ...location, number: " " },
      "number",
      M.numberRequired,
    ],
    [
      "bairro vazio",
      locationSchema,
      { ...location, neighborhood: "" },
      "neighborhood",
      M.neighborhoodRequired,
    ],
    [
      "bairro com 1 caractere",
      locationSchema,
      { ...location, neighborhood: text(1) },
      "neighborhood",
      M.neighborhoodLength,
    ],
    [
      "bairro com 101 caracteres",
      locationSchema,
      { ...location, neighborhood: text(101) },
      "neighborhood",
      M.neighborhoodLength,
    ],
    ["cidade vazia", locationSchema, { ...location, city: " " }, "city", M.cityRequired],
    [
      "UF não escolhida",
      locationSchema,
      { ...location, state: "" },
      "state",
      M.stateRequired,
    ],
    [
      "UF fora das 27",
      locationSchema,
      { ...location, state: "XX" },
      "state",
      M.stateRequired,
    ],
    [
      "modelo não escolhido",
      equipmentSchema(TODAY),
      { ...equipment, modelId: "" },
      "modelId",
      M.modelRequired,
    ],
    [
      "número de série vazio",
      equipmentSchema(TODAY),
      { ...equipment, serialNumber: " " },
      "serialNumber",
      M.serialNumberRequired,
    ],
    [
      "data de instalação amanhã",
      equipmentSchema(TODAY),
      { ...equipment, installedOn: "2026-11-25" },
      "installedOn",
      M.installedOnFuture,
    ],
  ] as const)("%s", (_, schema, values, path, message) => {
    expect(messageAt(schema, values, path)).toBe(message)
  })
})

describe("limites aceitos", () => {
  it.each([
    ["CPF válido, só e-mail", customerSchema, person],
    [
      "CNPJ alfanumérico, nome fantasia e só telefone",
      customerSchema,
      { ...company, email: "", phone: "(11) 99999-9999" },
    ],
    [
      "nome e nome fantasia com 2 caracteres",
      customerSchema,
      { ...company, name: text(2), tradeName: text(2) },
    ],
    [
      "nome e nome fantasia com 150 caracteres",
      customerSchema,
      { ...company, name: text(150), tradeName: text(150) },
    ],
    ["telefone fixo", customerSchema, { ...person, email: "", phone: "(34) 3212-4580" }],
    [
      "telefone estrangeiro guardado",
      customerSchema,
      { ...person, email: "", phone: "+14155552671" },
    ],
    ["nome do local com 2 caracteres", locationSchema, { ...location, name: text(2) }],
    ["nome do local com 60 caracteres", locationSchema, { ...location, name: text(60) }],
    ["CEP sem traço", locationSchema, { ...location, zipCode: "01310100" }],
    [
      "logradouro com 2 e bairro com 2",
      locationSchema,
      { ...location, street: text(2), neighborhood: text(2) },
    ],
    [
      "logradouro com 150 e bairro com 100",
      locationSchema,
      { ...location, street: text(150), neighborhood: text(100) },
    ],
    [
      "número S/N e cidade com 1 caractere",
      locationSchema,
      { ...location, number: "S/N", city: text(1) },
    ],
    ["equipamento sem data", equipmentSchema(TODAY), equipment],
    [
      "série com 1 caractere e instalado hoje",
      equipmentSchema(TODAY),
      { ...equipment, serialNumber: "X", installedOn: TODAY },
    ],
  ] as const)("%s", (_, schema, values) => {
    expect(schema.safeParse(values).success).toBe(true)
  })
})

describe("regras sem linha própria na tabela", () => {
  it("nome fantasia com CPF é recusado com a mensagem de trade_name_not_allowed", () => {
    expect(messageAt(customerSchema, { ...person, tradeName: "Ana" }, "tradeName")).toBe(
      "Nome fantasia só vale para pessoa jurídica.",
    )
  })

  it("apara as pontas do texto", () => {
    const parsed = customerSchema.parse({ ...company, name: "  Padaria LTDA  " })
    const parsedLocation = locationSchema.parse({ ...location, street: " Rua A " })

    expect(parsed.name).toBe("Padaria LTDA")
    expect(parsedLocation.street).toBe("Rua A")
  })
})
