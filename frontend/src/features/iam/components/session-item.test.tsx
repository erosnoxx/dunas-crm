import { render, screen } from "@testing-library/react"
import { describe, expect, it } from "vitest"

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
