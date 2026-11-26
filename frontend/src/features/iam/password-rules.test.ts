import { describe, expect, it } from "vitest"

import {
  checkPassword,
  isPasswordValid,
  PASSWORD_MAX,
  type PasswordRule,
} from "./password-rules"

function failed(password: string): PasswordRule["id"][] {
  return checkPassword(password)
    .filter((rule) => !rule.met)
    .map((rule) => rule.id)
}

// Os mesmos casos do PasswordTest do backend.
describe("checkPassword", () => {
  it("devolve as 5 regras, na ordem do mockup", () => {
    expect(checkPassword("").map((rule) => [rule.id, rule.label])).toEqual([
      ["length", "De 8 a 30 caracteres"],
      ["upper", "Uma letra maiúscula"],
      ["lower", "Uma letra minúscula"],
      ["digit", "Um número"],
      ["special", "Um caractere especial, como ! ou #"],
    ])
  })

  it("aceita Dunas#2026", () => {
    expect(failed("Dunas#2026")).toEqual([])
    expect(isPasswordValid("Dunas#2026")).toBe(true)
  })

  it("recusa 7 e 31 caracteres pelo tamanho", () => {
    expect(failed("Dunas#1")).toEqual(["length"])
    expect(failed(`Dunas#2026${"a".repeat(PASSWORD_MAX - 9)}`)).toEqual(["length"])
    expect(PASSWORD_MAX).toBe(30)
  })

  it("aceita 8 e 30 caracteres", () => {
    expect(failed("Dunas#20")).toEqual([])
    expect(failed(`Dunas#20${"a".repeat(22)}`)).toEqual([])
  })

  it("recusa pela regra certa quando falta um tipo de caractere", () => {
    expect(failed("dunas#2026")).toEqual(["upper"])
    expect(failed("DUNAS#2026")).toEqual(["lower"])
    expect(failed("Dunas#Dunas")).toEqual(["digit"])
    expect(failed("Dunas20260")).toEqual(["special"])
  })

  it("aceita Açúcar1€: ç é minúscula e € é especial", () => {
    expect(failed("Açúcar1€")).toEqual([])
  })

  it("não conta espaço como caractere especial", () => {
    expect(failed("Aa1     ")).toEqual(["special"])
  })

  it("não apara a senha: mede o texto como veio", () => {
    expect(isPasswordValid(" Dunas#2026 ")).toBe(true)
    expect(failed("  Dunas#1")).toEqual([])
    expect(failed(" Dunas#1 ".trim())).toEqual(["length"])
  })

  it("segue Character.isUpperCase/isLowerCase do Java, inclusive Other_Uppercase/Other_Lowercase", () => {
    expect(failed("Ⅰunas#2026")).toEqual([])
    expect(failed("DUNAⓐ#2026")).toEqual([])
  })

  it("não valida senha vazia", () => {
    expect(isPasswordValid("")).toBe(false)
  })
})
