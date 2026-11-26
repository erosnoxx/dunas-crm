import * as React from "react"

import { cn } from "@/shared/lib/cn"

import { LABELS, ROLES } from "../iam.messages"
import type { Role } from "../iam.service"

type RolePickerProps = {
  value?: Role
  onChange: (role: Role) => void
}

/**
 * As três opções de papel como rádio nativo (teclado e leitor de tela de graça). A linha
 * escolhida leva o fundo `chip-ok` e a borda `brand`; as outras, `line-strong`. Sobre o
 * painel (a partir de `md`) as linhas usam `ground`, como no mockup.
 */
function RolePicker({ value, onChange }: RolePickerProps) {
  const name = React.useId()

  return (
    <fieldset role="radiogroup" className="flex flex-col gap-2">
      <legend className="pb-1.5 text-label text-text">Papel</legend>
      {ROLES.map((role) => (
        <label
          key={role}
          className={cn(
            "flex min-h-13 cursor-pointer items-center gap-3 border px-3.5 md:min-h-12",
            role === value
              ? "border-brand bg-chip-ok"
              : "border-line-strong bg-panel md:bg-ground",
          )}
        >
          <input
            type="radio"
            name={name}
            value={role}
            checked={role === value}
            onChange={() => onChange(role)}
            className="m-0 size-5 accent-brand"
          />
          <span className="text-body-sm font-semibold">{LABELS.role[role]}</span>
        </label>
      ))}
    </fieldset>
  )
}

export { RolePicker }
