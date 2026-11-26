import { readFileSync } from "node:fs"
import { join } from "node:path"
import { useQueryClient } from "@tanstack/react-query"
import { render, screen } from "@testing-library/react"
import { createRoutesStub } from "react-router"
import { describe, expect, it } from "vitest"

import App from "./root"

// `useQueryClient` lança sem o provider: a rota só renderiza se os providers envolvem o `Outlet`.
function Probe() {
  useQueryClient()

  return <p>rota dentro dos providers</p>
}

describe("raiz da aplicação", () => {
  it("renderiza a rota inicial dentro dos providers", () => {
    const Stub = createRoutesStub([
      { path: "/", Component: App, children: [{ index: true, Component: Probe }] },
    ])

    render(<Stub initialEntries={["/"]} />)

    expect(screen.getByText("rota dentro dos providers")).toBeInTheDocument()
  })

  it("declara o idioma pt-BR e a viewport do celular", () => {
    const source = readFileSync(join(process.cwd(), "src/app/root.tsx"), "utf8")

    expect(source).toContain('lang="pt-BR"')
    expect(source).toContain("width=device-width")
  })
})
