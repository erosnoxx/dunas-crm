import * as React from "react"

import { cn } from "@/shared/lib/cn"

type Tone = "ok" | "pending" | "off"
type Shape = "filled" | "ring" | "slashed"

const SHAPE: Record<Tone, Shape> = {
  ok: "filled",
  pending: "ring",
  off: "slashed",
}

/**
 * Situação lida sem cor (design system, princípio 4): círculo cheio, vazado ou cortado, além
 * da diferença de fundo e de contorno.
 */
function Glyph({ shape }: { shape: Shape }) {
  return (
    <svg
      width="10"
      height="10"
      viewBox="0 0 10 10"
      fill={shape === "filled" ? "currentColor" : "none"}
      stroke="currentColor"
      strokeWidth="1.6"
      aria-hidden="true"
      data-shape={shape}
    >
      <circle cx="5" cy="5" r={shape === "filled" ? 5 : 4} />
      {shape === "slashed" && <path d="M2 8l6-6" />}
    </svg>
  )
}

/** Chip de situação. `ok` é o fundo `chip-ok`; as demais têm contorno (duas camadas). */
function StatusChip({ tone, children }: { tone: Tone; children: React.ReactNode }) {
  const label = (
    <span
      className={cn(
        "cut-sm inline-flex items-center gap-2 px-3 text-label",
        tone === "ok" ? "h-[30px] bg-chip-ok text-text" : "h-7 bg-panel",
        tone === "off" ? "text-text-muted" : "text-text",
      )}
    >
      <Glyph shape={SHAPE[tone]} />
      {children}
    </span>
  )

  if (tone === "ok") return label

  return <span className="cut-sm inline-flex bg-line-strong p-px">{label}</span>
}

export { StatusChip }
