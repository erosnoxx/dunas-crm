import { render, screen } from "@testing-library/react"
import { createRoutesStub } from "react-router"
import { describe, expect, it } from "vitest"

import { CatalogTabs } from "./catalog-tabs"

function renderTabs(active: "models" | "manufacturers" | "types") {
  const Stub = createRoutesStub([
    { path: "/catalogo", Component: () => <CatalogTabs active={active} /> },
  ])

  render(<Stub initialEntries={["/catalogo"]} />)
}

describe("CatalogTabs", () => {
  it("são três links do catálogo, em ordem", async () => {
    renderTabs("models")

    const nav = await screen.findByRole("navigation", { name: "Seções do catálogo" })
    const links = Array.from(nav.querySelectorAll("a"))

    expect(links.map((link) => [link.textContent, link.getAttribute("href")])).toEqual([
      ["Modelos", "/catalogo"],
      ["Fabricantes", "/catalogo/fabricantes"],
      ["Tipos", "/catalogo/tipos"],
    ])
  })

  it.each([
    ["models", "Modelos"],
    ["manufacturers", "Fabricantes"],
    ["types", "Tipos"],
  ] as const)("%s: só a aba ativa leva aria-current", async (active, label) => {
    renderTabs(active)

    await screen.findByRole("navigation", { name: "Seções do catálogo" })
    const current = screen.getAllByRole("link").filter((link) => link.ariaCurrent)

    expect(current).toHaveLength(1)
    expect(current[0]).toHaveTextContent(label)
    expect(current[0]).toHaveAttribute("aria-current", "page")
  })

  it("a ativa usa brand-text com traço de 3 px na web", async () => {
    renderTabs("types")

    expect(await screen.findByRole("link", { name: "Tipos" })).toHaveClass(
      "md:aria-[current=page]:border-brand-text",
      "md:aria-[current=page]:text-brand-text",
      "md:border-b-3",
    )
  })
})
