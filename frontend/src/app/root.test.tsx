import { readFileSync } from "node:fs"
import { join } from "node:path"
import { render, screen } from "@testing-library/react"
import { createRoutesStub } from "react-router"
import { describe, expect, it } from "vitest"

import App from "./root"
import Home from "./routes/home"

describe("raiz da aplicação", () => {
  it("renderiza a rota inicial dentro dos providers", () => {
    const Stub = createRoutesStub([
      { path: "/", Component: App, children: [{ index: true, Component: Home }] },
    ])

    render(<Stub initialEntries={["/"]} />)

    expect(screen.getByRole("img", { name: "DUNAS" })).toBeInTheDocument()
    expect(screen.getByText("Em construção")).toBeInTheDocument()
  })

  it("declara o idioma pt-BR e a viewport do celular", () => {
    const source = readFileSync(join(process.cwd(), "src/app/root.tsx"), "utf8")

    expect(source).toContain('lang="pt-BR"')
    expect(source).toContain("width=device-width")
  })
})
