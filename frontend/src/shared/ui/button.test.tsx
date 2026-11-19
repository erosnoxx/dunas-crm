import { render, screen } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import { describe, expect, it, vi } from "vitest"

import { Button } from "./button"

describe("Button", () => {
  it("o principal é laranja com texto escuro, nunca branco", () => {
    render(<Button>Entrar</Button>)

    const button = screen.getByRole("button", { name: "Entrar" })

    expect(button).toHaveClass("bg-brand", "text-on-brand", "cut-sm")
    expect(button).not.toHaveClass("text-white")
  })

  it("o secundário tem contorno de duas camadas", () => {
    render(<Button variant="outline">Cancelar</Button>)

    const button = screen.getByRole("button", { name: "Cancelar" })

    expect(button).toHaveClass("bg-line-strong")
    expect(button.firstElementChild).toHaveClass("bg-ground", "cut-sm")
  })

  it("no tamanho do celular o corte é o maior e o alvo de toque tem 52 px", () => {
    render(<Button size="lg">Entrar</Button>)

    expect(screen.getByRole("button")).toHaveClass("cut-md", "min-h-13")
  })

  it("o alvo de toque padrão tem pelo menos 44 px", () => {
    render(<Button>Entrar</Button>)

    expect(screen.getByRole("button")).toHaveClass("min-h-11")
  })

  it("carregando desabilita, avisa as tecnologias assistivas e não dispara o clique", async () => {
    const onClick = vi.fn()
    render(
      <Button loading onClick={onClick}>
        Entrar
      </Button>,
    )

    const button = screen.getByRole("button", { name: "Entrar" })
    await userEvent.click(button)

    expect(button).toHaveAttribute("aria-busy", "true")
    expect(button).toHaveAttribute("data-loading", "true")
    expect(onClick).not.toHaveBeenCalled()
  })

  it("o texto de perigo usa o token de texto de erro", () => {
    render(<Button variant="danger-text">Desativar usuário</Button>)

    expect(screen.getByRole("button")).toHaveClass("text-danger-text")
  })
})
