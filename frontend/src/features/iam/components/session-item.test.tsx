import { render, screen } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import { describe, expect, it, vi } from "vitest"

import type { SessionSummary } from "../iam.service"
import { SessionItem } from "./session-item"

const NOW = new Date(2026, 10, 19, 12, 0, 0)
const MINUTE = 60_000

function lastUse(msAgo: number): string {
  const lastUsedAt = new Date(NOW.getTime() - msAgo).toISOString()
  const session: SessionSummary = {
    id: "s",
    device: "Chrome, Windows",
    startedAt: new Date(2026, 0, 5, 9).toISOString(),
    lastUsedAt,
    current: false,
  }

  render(
    <ul>
      <SessionItem
        session={session}
        now={NOW}
        pending={false}
        onRevoke={() => undefined}
      />
    </ul>,
  )

  return screen.getByText(/^Último uso: /).textContent ?? ""
}

describe("SessionItem: último uso relativo", () => {
  it.each([
    [10_000, "Último uso: agora"],
    [-5 * MINUTE, "Último uso: agora"],
    [5 * MINUTE, "Último uso: há 5 min"],
    [60 * MINUTE, "Último uso: há 1 hora"],
    [3 * 60 * MINUTE, "Último uso: há 3 horas"],
    [24 * 60 * MINUTE, "Último uso: há 1 dia"],
    [9 * 24 * 60 * MINUTE, "Último uso: há 9 dias"],
  ])("%d ms atrás: %s", (msAgo, expected) => {
    expect(lastUse(msAgo)).toBe(expected)
  })

  it("a data de entrada é dia/mês com zero à esquerda", () => {
    lastUse(10_000)

    expect(screen.getByText("Entrou em 05/01")).toBeInTheDocument()
  })
})

describe("SessionItem: disposição no painel do usuário", () => {
  const base: SessionSummary = {
    id: "s",
    device: "Galaxy A54, app DUNAS",
    startedAt: new Date(2026, 0, 5, 9).toISOString(),
    lastUsedAt: new Date(NOW.getTime() - 2 * MINUTE).toISOString(),
    current: false,
  }
  const renderItem = (
    layout: "card" | "stacked" | "inline",
    onRevoke = () => undefined,
  ) =>
    render(
      <ul>
        <SessionItem
          session={base}
          now={NOW}
          pending={false}
          onRevoke={onRevoke}
          layout={layout}
        />
      </ul>,
    )

  it("inline: Encerrar ao lado do aparelho, sem a data de entrada", async () => {
    const onRevoke = vi.fn()
    renderItem("inline", onRevoke)

    await userEvent.click(
      screen.getByRole("button", { name: "Encerrar, Galaxy A54, app DUNAS" }),
    )

    expect(onRevoke).toHaveBeenCalledWith("s")
    expect(screen.getByText("Último uso: há 2 min")).toBeVisible()
    expect(screen.queryByText(/Entrou em/)).not.toBeInTheDocument()
  })

  it("inline: o ícone fica centrado com o texto (sem a margem do ícone empilhado)", () => {
    renderItem("inline")

    expect(screen.getByRole("listitem").querySelector("svg")).not.toHaveClass("mt-0.5")
  })

  it("stacked: sem cartão, com a linha de baixo, e o botão do próprio usuário", () => {
    renderItem("stacked")

    expect(screen.getByRole("listitem").querySelector("svg")).toHaveClass("mt-0.5")
    expect(screen.getByRole("listitem")).toHaveClass("border-b")
    expect(screen.getByRole("listitem")).not.toHaveClass("bg-panel")
    expect(screen.getByRole("button", { name: "Encerrar esta sessão" })).toBeVisible()
  })

  it("card é o padrão da tela Minhas sessões", () => {
    renderItem("card")

    expect(screen.getByRole("listitem")).toHaveClass("cut-lg", "bg-panel")
  })
})
