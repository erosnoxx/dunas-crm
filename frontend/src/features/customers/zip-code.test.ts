import { describe, expect, it } from "vitest"

import { formatZipCode, maskZipCode } from "./zip-code"

describe("maskZipCode", () => {
  it.each([
    ["", ""],
    ["0131", "0131"],
    ["01310", "01310"],
    ["013101", "01310-1"],
    ["01310100", "01310-100"],
    ["01310-1009", "01310-100"],
    ["01a310", "01310"],
  ])("%j vira %j", (text, masked) => {
    expect(maskZipCode(text)).toBe(masked)
  })
})

describe("formatZipCode", () => {
  it("o CEP guardado volta com o traço", () => {
    expect(formatZipCode("01310100")).toBe("01310-100")
    expect(formatZipCode("00000000")).toBe("00000-000")
  })
})
