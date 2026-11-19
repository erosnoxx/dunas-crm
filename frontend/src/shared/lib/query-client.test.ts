import { describe, expect, it } from "vitest"

import { shouldRetry } from "./query-client"

describe("shouldRetry", () => {
  it("não repete erro de negócio (4xx)", () => {
    expect(shouldRetry(0, { status: 404, message: "não encontrado" })).toBe(false)
  })

  it("repete uma vez a falha de rede", () => {
    expect(shouldRetry(0, { status: 0, message: "rede" })).toBe(true)
    expect(shouldRetry(1, { status: 0, message: "rede" })).toBe(false)
  })
})
