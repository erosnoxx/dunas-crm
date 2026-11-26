import { render, screen } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import { useState } from "react"
import { describe, expect, it, vi } from "vitest"

import type { Role } from "../iam.service"
import { RolePicker } from "./role-picker"

function Harness({ initial }: { initial?: Role }) {
  const [value, setValue] = useState<Role | undefined>(initial)
  return <RolePicker value={value} onChange={setValue} />
}

describe("RolePicker", () => {
  it("é um grupo de rádio chamado Papel com as três opções", () => {
    render(<Harness />)

    const group = screen.getByRole("radiogroup", { name: "Papel" })
    expect(group).toBeInTheDocument()
    expect(screen.getAllByRole("radio").map((r) => r.parentElement?.textContent)).toEqual(
      ["Administrador", "Comercial", "Técnico"],
    )
  })

  it("marca só a opção escolhida, com fundo chip-ok e borda brand", () => {
    render(<Harness initial="COMMERCIAL" />)

    expect(screen.getByRole("radio", { name: "Comercial" })).toBeChecked()
    expect(screen.getByRole("radio", { name: "Técnico" })).not.toBeChecked()
    expect(screen.getByText("Comercial").closest("label")).toHaveClass(
      "bg-chip-ok",
      "border-brand",
    )
    expect(screen.getByText("Técnico").closest("label")).toHaveClass("border-line-strong")
  })

  it("navega por teclado e avisa a escolha", async () => {
    const user = userEvent.setup()
    const onChange = vi.fn()
    render(<RolePicker value="ADMINISTRATOR" onChange={onChange} />)

    await user.tab()
    expect(screen.getByRole("radio", { name: "Administrador" })).toHaveFocus()

    await user.keyboard("{ArrowDown}")
    expect(onChange).toHaveBeenCalledWith("COMMERCIAL")
  })

  it("clicar na linha escolhe o papel", async () => {
    const user = userEvent.setup()
    render(<Harness />)

    await user.click(screen.getByText("Técnico"))

    expect(screen.getByRole("radio", { name: "Técnico" })).toBeChecked()
  })
})
