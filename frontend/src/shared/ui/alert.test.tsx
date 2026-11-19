import { render, screen } from "@testing-library/react"
import { describe, expect, it } from "vitest"

import { Alert } from "./alert"

describe("Alert", () => {
  it("o erro é contorno com texto de erro e é anunciado", () => {
    render(<Alert tone="danger">e-mail ou senha incorretos</Alert>)

    const alert = screen.getByRole("alert")

    expect(alert).toHaveClass("border-danger", "text-danger-text")
    expect(alert).toHaveTextContent("e-mail ou senha incorretos")
  })

  it("o aviso é o painel de destaque com texto escuro", () => {
    render(
      <Alert tone="warning" title="Muitas tentativas de entrada">
        o acesso fica pausado por 15 minutos
      </Alert>,
    )

    const alert = screen.getByRole("alert")

    expect(alert).toHaveClass("bg-highlight", "text-on-highlight", "cut-lg")
    expect(alert).toHaveTextContent("Muitas tentativas de entrada")
  })
})
