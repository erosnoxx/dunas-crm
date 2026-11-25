import { describe, expect, it } from "vitest"

import { FORM_MESSAGES } from "./customers.messages"
import {
  checkDocument,
  documentTypeOf,
  formatDocument,
  normalizeDocument,
} from "./document"

// Valores dos testes do `Document` do backend.
const CPF = "12345678909"
const CNPJ = "11222333000181"
const ALPHANUMERIC_CNPJ = "12ABC34501DE35"

describe("normalizeDocument", () => {
  it("tira a pontuação e põe as letras em maiúsculas", () => {
    expect(normalizeDocument("123.456.789-09")).toBe(CPF)
    expect(normalizeDocument("12.abc.345/01de-35")).toBe(ALPHANUMERIC_CNPJ)
  })
})

describe("documentTypeOf", () => {
  it("11 caracteres é CPF, 14 é CNPJ, outro tamanho não tem tipo", () => {
    expect(documentTypeOf("123.456.789-09")).toBe("CPF")
    expect(documentTypeOf("12.ABC.345/01DE-35")).toBe("CNPJ")
    expect(documentTypeOf("1234567890")).toBeUndefined()
    expect(documentTypeOf("")).toBeUndefined()
  })
})

describe("checkDocument", () => {
  it.each([
    ["123.456.789-09", CPF],
    [CPF, CPF],
    ["11.222.333/0001-81", CNPJ],
    ["12.ABC.345/01DE-35", ALPHANUMERIC_CNPJ],
    ["12.abc.345/01de-35", ALPHANUMERIC_CNPJ],
  ])("aceita %s e devolve %s", (text, value) => {
    expect(checkDocument(text)).toEqual({ ok: true, value })
  })

  it.each([
    ["", FORM_MESSAGES.documentRequired],
    ["   ", FORM_MESSAGES.documentRequired],
    ["1234567890", FORM_MESSAGES.documentIncomplete],
    ["123456789012", FORM_MESSAGES.documentIncomplete],
    ["123456789012345", FORM_MESSAGES.documentIncomplete],
    ["123.456.789-08", FORM_MESSAGES.cpfInvalid],
    ["11111111111", FORM_MESSAGES.cpfInvalid],
    ["1234567890A", FORM_MESSAGES.cpfInvalid],
    ["11.222.333/0001-82", FORM_MESSAGES.cnpjInvalid],
    ["12.ABC.345/01DE-36", FORM_MESSAGES.cnpjInvalid],
    ["11111111111111", FORM_MESSAGES.cnpjInvalid],
    ["AAAAAAAAAAAAAA", FORM_MESSAGES.cnpjInvalid],
    ["12ABC34501DE3A", FORM_MESSAGES.cnpjInvalid],
  ])("recusa %j com a mensagem certa", (text, message) => {
    expect(checkDocument(text)).toEqual({ ok: false, message })
  })

  it("as mensagens são as da tabela", () => {
    expect(checkDocument("1234567890")).toEqual({
      ok: false,
      message: "Documento incompleto: o CPF tem 11 dígitos e o CNPJ, 14 caracteres.",
    })
    expect(checkDocument("123.456.789-08")).toEqual({
      ok: false,
      message: "CPF inválido: confira os números digitados.",
    })
    expect(checkDocument("11.222.333/0001-82")).toEqual({
      ok: false,
      message: "CNPJ inválido: confira os caracteres digitados.",
    })
  })
})

describe("formatDocument", () => {
  it.each([
    ["", ""],
    ["123", "123"],
    ["1234", "123.4"],
    ["1234567", "123.456.7"],
    ["1234567890", "123.456.789-0"],
    [CPF, "123.456.789-09"],
    ["123456789012", "12.345.678/9012"],
    ["1234567890123", "12.345.678/9012-3"],
    [CNPJ, "11.222.333/0001-81"],
    ["12abc", "12.ABC"],
    [ALPHANUMERIC_CNPJ, "12.ABC.345/01DE-35"],
    ["12.ABC.345/01DE-35", "12.ABC.345/01DE-35"],
    ["112223330001819", "11.222.333/0001-81"],
  ])("%j vira %j", (text, formatted) => {
    expect(formatDocument(text)).toBe(formatted)
  })
})
