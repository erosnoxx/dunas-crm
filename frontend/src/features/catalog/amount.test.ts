import { describe, expect, it } from "vitest"

import { formatAmount, parseAmount } from "./amount"

describe("parseAmount", () => {
  it.each([
    ["3", "3"],
    ["0,5", "0.5"],
    ["12000", "12000"],
    ["999999999,999", "999999999.999"],
    ["1,25", "1.25"],
  ])("aceita %s e devolve %s com ponto", (input, value) => {
    expect(parseAmount(input)).toEqual({ ok: true, value })
  })

  it.each([
    "12.000",
    "1,2345",
    "0",
    "0,000",
    "-1",
    "+1",
    "1e3",
    "1 000",
    "",
    "1,",
    ",5",
    "1,2,3",
    "1000000000",
  ])("recusa %j com uma razão", (input) => {
    const result = parseAmount(input)

    expect(result.ok).toBe(false)
    if (!result.ok) expect(result.reason.length).toBeGreaterThan(0)
  })
})

describe("formatAmount", () => {
  it.each([
    ["3", "3"],
    [3, "3"],
    [0.5, "0,5"],
    ["0.500", "0,5"],
    ["12000", "12000"],
    [12000, "12000"],
    ["12000.000", "12000"],
    ["1.25", "1,25"],
  ])("%j vira %j", (input, text) => {
    expect(formatAmount(input)).toBe(text)
  })
})
