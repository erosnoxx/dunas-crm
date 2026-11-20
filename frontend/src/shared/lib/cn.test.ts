import { describe, expect, it } from "vitest"

import { cn } from "./cn"

describe("cn", () => {
  it("mantém o tamanho da escala ao lado de uma cor de texto", () => {
    expect(cn("text-label text-text")).toBe("text-label text-text")
    expect(cn("text-body-sm text-danger-text")).toBe("text-body-sm text-danger-text")
  })

  it("o tamanho da escala que vem depois vence o anterior", () => {
    expect(cn("text-body", "text-body-sm")).toBe("text-body-sm")
    expect(cn("text-display-lg", "md:text-title", "text-title")).toBe(
      "md:text-title text-title",
    )
  })

  it("a cor que vem depois vence a anterior", () => {
    expect(cn("text-text", "text-text-muted")).toBe("text-text-muted")
  })
})
