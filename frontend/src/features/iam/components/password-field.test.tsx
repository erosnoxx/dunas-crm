import { render, screen } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import { describe, expect, it } from "vitest"

import { PasswordField } from "./password-field"

describe("PasswordField", () => {
  it("mostra e oculta o texto pelo botão, que informa o estado com aria-pressed", async () => {
    const user = userEvent.setup()
    render(<PasswordField id="senha" label="Senha" />)

    const input = screen.getByLabelText("Senha")
    expect(input).toHaveAttribute("type", "password")

    const toggle = screen.getByRole("button", { name: "Mostrar" })
    expect(toggle).toHaveAttribute("aria-pressed", "false")

    await user.click(toggle)
    expect(input).toHaveAttribute("type", "text")
    expect(screen.getByRole("button", { name: "Ocultar" })).toHaveAttribute(
      "aria-pressed",
      "true",
    )

    await user.click(screen.getByRole("button", { name: "Ocultar" }))
    expect(input).toHaveAttribute("type", "password")
  })

  it("repassa maxLength=30 e colar 31 caracteres deixa 30, sem aparar", async () => {
    const user = userEvent.setup()
    render(<PasswordField id="senha" label="Senha" />)

    const input = screen.getByLabelText("Senha")
    expect(input).toHaveAttribute("maxlength", "30")

    await user.click(input)
    await user.paste(` ${"a".repeat(30)}`)

    expect(input).toHaveValue(` ${"a".repeat(29)}`)
  })

  it("repassa as demais propriedades ao campo", () => {
    render(<PasswordField id="senha" label="Senha" autoComplete="new-password" />)

    expect(screen.getByLabelText("Senha")).toHaveAttribute("autocomplete", "new-password")
  })

  it("o botão tem alvo de toque de 44 px ou mais", () => {
    render(<PasswordField id="senha" label="Senha" />)

    expect(screen.getByRole("button", { name: "Mostrar" })).toHaveClass("min-h-11")
  })

  it("mostra o erro ligado ao campo", () => {
    render(
      <PasswordField id="senha" label="Senha" error="a senha não cumpre as regras" />,
    )

    const input = screen.getByLabelText("Senha")
    expect(input).toHaveAttribute("aria-invalid", "true")
    expect(input).toHaveAccessibleDescription("a senha não cumpre as regras")
  })

  it("mostra o contador N de 30 quando pedido", () => {
    const { rerender } = render(
      <PasswordField
        id="senha"
        label="Senha"
        showCount
        value="Dunasolar"
        onChange={() => {}}
      />,
    )
    expect(screen.getByText("9 de 30")).toBeInTheDocument()

    rerender(<PasswordField id="senha" label="Senha" value="" onChange={() => {}} />)
    expect(screen.queryByText(/de 30/)).not.toBeInTheDocument()
  })

  it("o limite de 30 não pode ser trocado por quem usa o campo", () => {
    render(<PasswordField id="senha" label="Senha" maxLength={100} />)

    expect(screen.getByLabelText("Senha")).toHaveAttribute("maxlength", "30")
  })

  it("junta o aria-describedby de quem usa com o do erro", () => {
    render(
      <>
        <p id="dica">use a senha do convite</p>
        <PasswordField
          id="senha"
          label="Senha"
          aria-describedby="dica"
          error="informe a senha"
        />
      </>,
    )

    expect(screen.getByLabelText("Senha")).toHaveAccessibleDescription(
      "use a senha do convite informe a senha",
    )
  })

  it("desligado, desliga também o botão de mostrar", () => {
    render(<PasswordField id="senha" label="Senha" disabled />)

    expect(screen.getByLabelText("Senha")).toBeDisabled()
    expect(screen.getByRole("button", { name: "Mostrar" })).toBeDisabled()
  })
})
