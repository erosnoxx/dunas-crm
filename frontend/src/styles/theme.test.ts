import { readFileSync } from "node:fs"
import { fileURLToPath } from "node:url"
import { describe, expect, it } from "vitest"

type Token = { name: string; value: { dark: string; light: string } }
type Style = { name: string; fontSize: string; lineHeight: string; fontWeight: number }

const read = (path: string) =>
  readFileSync(fileURLToPath(new URL(path, import.meta.url)), "utf8")

const css = read("./theme.css")
const tokens = JSON.parse(read("../../../docs/design-system/tokens.json"))

const LIGHT = "@media (prefers-color-scheme: light)"

function variables(block: string): Map<string, string> {
  return new Map(
    Array.from(block.matchAll(/--([\w-]+):\s*(#[0-9a-fA-F]+);/g), ([, name, value]) => [
      name,
      value.toLowerCase(),
    ]),
  )
}

const dark = variables(css.slice(css.indexOf(":root {"), css.indexOf(LIGHT)))
const light = new Map([
  ...dark,
  ...variables(css.slice(css.indexOf(LIGHT), css.indexOf("@theme inline"))),
])

const colors: Token[] = tokens.color.tokens
const styles: Style[] = tokens.type.groups.flatMap(
  (group: { styles: Style[] }) => group.styles,
)
const cuts: { name: string; value: string }[] = tokens.radius.tokens.filter(
  (radius: { name: string }) => radius.name.startsWith("cut-"),
)

function utility(name: string): string {
  const start = css.indexOf(`@utility ${name} {`)
  return start < 0 ? "" : css.slice(start, css.indexOf("}", start))
}

describe("tema DUNAS", () => {
  it("traz todas as cores do design system", () => {
    expect(colors.length).toBeGreaterThan(10)
  })

  it.each(colors.map((token) => [token.name, token] as const))(
    "%s tem o valor de tokens.json no tema escuro e no claro",
    (name, token) => {
      expect(dark.get(name)).toBe(token.value.dark.toLowerCase())
      expect(light.get(name)).toBe(token.value.light.toLowerCase())
    },
  )

  it.each(styles.map((style) => [style.name, style] as const))(
    "o estilo de texto %s tem tamanho, altura de linha e peso do design system",
    (name, style) => {
      const block = utility(`text-${name}`)

      expect(block).toContain(`font-size: ${style.fontSize};`)
      expect(block).toContain(`line-height: ${style.lineHeight};`)
      expect(block).toContain(`font-weight: ${style.fontWeight};`)
    },
  )

  it.each(cuts.map((cut) => [cut.name, cut.value] as const))(
    "o corte %s tem o tamanho do design system",
    (name, size) => {
      expect(utility(name)).toContain(`calc(100% - ${size}) 0, 100% ${size}`)
    },
  )
})

describe("fontes", () => {
  const fonts = read("./fonts.ts")

  it("vêm de pacotes @fontsource, sem Google Fonts", () => {
    expect(fonts).toContain("@fontsource/chakra-petch")
    expect(fonts).toContain("@fontsource/ibm-plex-sans")
    expect(fonts).not.toContain("googleapis")
  })
})

// Todo código do app, menos os testes.
const sources = import.meta.glob(["../**/*.{ts,tsx}", "!../**/*.test.{ts,tsx}"], {
  query: "?raw",
  import: "default",
  eager: true,
}) as Record<string, string>

describe("código do app", () => {
  it.each(Object.entries(sources))("%s não tem cor literal", (_path, source) => {
    expect(source).not.toMatch(/#[0-9a-fA-F]{3,8}\b/)
    expect(source).not.toMatch(/\brgba?\(/)
  })
})
