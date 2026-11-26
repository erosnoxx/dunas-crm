import { describe, expect, it } from "vitest"

import { checkPhone, formatPhone, isBrazilian, maskPhone, toE164 } from "./phone"

describe("maskPhone", () => {
  it.each([
    ["", ""],
    ["3", "(3"],
    ["34", "(34"],
    ["343", "(34) 3"],
    ["343212", "(34) 3212"],
    ["3432124580", "(34) 3212-4580"],
    ["11999999999", "(11) 99999-9999"],
    ["(11) 99999-99999", "(11) 99999-9999"],
    ["11a9", "(11) 9"],
  ])("%j vira %j", (text, masked) => {
    expect(maskPhone(text)).toBe(masked)
  })
})

describe("toE164", () => {
  it("envia +55, DDD e número", () => {
    expect(toE164("(34) 3212-4580")).toBe("+553432124580")
    expect(toE164("(11) 99999-9999")).toBe("+5511999999999")
  })

  it("vazio é null", () => {
    expect(toE164("")).toBeNull()
    expect(toE164("  ")).toBeNull()
  })

  it("o número estrangeiro guardado volta sem mudança", () => {
    expect(toE164("+14155552671")).toBe("+14155552671")
  })
})

describe("checkPhone", () => {
  it.each(["(34) 3212-4580", "(11) 99999-9999", "(99) 2345-6789", "+14155552671"])(
    "aceita %j",
    (text) => {
      expect(checkPhone(text)).toBe(true)
    },
  )

  it.each([
    ["(11) 89999-9999", "9 dígitos sem o 9 inicial"],
    ["(01) 3212-4580", "DDD com zero inicial"],
    ["(34) 321-4580", "7 dígitos"],
    ["(3", "sem número"],
    ["", "vazio"],
    ["+1", "E.164 curto demais"],
  ])("recusa %j (%s)", (text) => {
    expect(checkPhone(text)).toBe(false)
  })
})

describe("formatPhone e isBrazilian", () => {
  it("+55 volta formatado", () => {
    expect(formatPhone("+553432124580")).toBe("(34) 3212-4580")
    expect(formatPhone("+5511999999999")).toBe("(11) 99999-9999")
    expect(isBrazilian("+5511999999999")).toBe(true)
  })

  it("outro E.164 fica como está guardado", () => {
    expect(formatPhone("+14155552671")).toBe("+14155552671")
    expect(isBrazilian("+14155552671")).toBe(false)
  })
})
