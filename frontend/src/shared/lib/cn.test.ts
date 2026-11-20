import { readFileSync } from "node:fs"
import { fileURLToPath } from "node:url"
import { describe, expect, it } from "vitest"

import { cn, TYPE_SCALE } from "./cn"

const read = (path: string) =>
  readFileSync(fileURLToPath(new URL(path, import.meta.url)), "utf8")

const tokens = JSON.parse(read("../../../../docs/design-system/tokens.json"))
const styleNames: string[] = tokens.type.groups.flatMap(
  (group: { styles: { name: string }[] }) => group.styles.map((style) => style.name),
)
const cssUtilities = Array.from(
  read("../../styles/theme.css").matchAll(/@utility text-([\w-]+) \{/g),
  ([, name]) => name,
)

describe("cn", () => {
  it("registra como tamanho de fonte toda a escala do tokens.json e do theme.css", () => {
    expect([...TYPE_SCALE].sort()).toEqual([...styleNames].sort())
    expect([...TYPE_SCALE].sort()).toEqual([...cssUtilities].sort())
  })

  it.each(styleNames)("mantém text-%s ao lado de uma cor de texto", (name) => {
    expect(cn(`text-${name} text-text`)).toBe(`text-${name} text-text`)
    expect(cn(`text-${name} text-danger-text`)).toBe(`text-${name} text-danger-text`)
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
