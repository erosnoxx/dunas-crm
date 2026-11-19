import { describe, expect, it } from "vitest"

// Todo código do app, menos os testes.
const sources = import.meta.glob(["../../**/*.{ts,tsx}", "!../../**/*.test.{ts,tsx}"], {
  query: "?raw",
  import: "default",
  eager: true,
}) as Record<string, string>

// O cliente HTTP é o único lugar que pode falar de credencial (docs/rules/frontend.md §5).
const isHttpClient = (path: string) => path.endsWith("/api/http-client.ts")

describe("código do app", () => {
  it("encontrou o código-fonte para varrer", () => {
    expect(Object.keys(sources).some(isHttpClient)).toBe(true)
  })

  it.each(Object.entries(sources).filter(([path]) => !isHttpClient(path)))(
    "%s não guarda sessão no navegador nem mexe em Authorization",
    (_path, source) => {
      expect(source).not.toMatch(/localStorage|sessionStorage/)
      expect(source).not.toMatch(/["'`]Authorization["'`]/)
    },
  )
})
