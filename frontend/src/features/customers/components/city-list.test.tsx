import { render, screen } from "@testing-library/react"
import { describe, expect, it } from "vitest"

import { CityList } from "./city-list"

const CITIES = [
  { city: "Anápolis", state: "GO" as const },
  { city: "Goiânia", state: "GO" as const },
  { city: "Uberlândia", state: "MG" as const },
]

describe("CityList", () => {
  it("mostra duas cidades e '+1', com 'e mais 1 cidade' só para o leitor de tela", () => {
    const { container } = render(<CityList cities={CITIES} text="" />)

    expect(container).toHaveTextContent("Anápolis/GO, Goiânia/GO")
    expect(screen.getByText("+1")).toHaveAttribute("aria-hidden", "true")
    expect(screen.getByText("e mais 1 cidade")).toHaveClass("sr-only")
  })

  it("a cidade buscada vem primeiro", () => {
    const { container } = render(<CityList cities={CITIES} text="uberl" />)

    expect(container).toHaveTextContent("Uberlândia/MG, Anápolis/GO")
  })

  it("com mais de uma escondida, o plural", () => {
    render(<CityList cities={[...CITIES, { city: "Catalão", state: "GO" }]} text="" />)

    expect(screen.getByText("+2")).toBeInTheDocument()
    expect(screen.getByText("e mais 2 cidades")).toBeInTheDocument()
  })

  it("até duas não há '+N'", () => {
    const { container } = render(<CityList cities={CITIES.slice(0, 2)} text="" />)

    expect(container).toHaveTextContent(/^Anápolis\/GO, Goiânia\/GO$/)
  })

  it("sem cidade, 'Nenhum local'", () => {
    render(<CityList cities={[]} text="" />)

    expect(screen.getByText("Nenhum local")).toHaveClass("text-text-muted")
  })
})
