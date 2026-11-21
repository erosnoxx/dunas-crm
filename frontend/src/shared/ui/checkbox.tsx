import * as React from "react"

import { cn } from "@/shared/lib/cn"

type CheckboxProps = Omit<React.ComponentProps<"input">, "type" | "children"> & {
  label: React.ReactNode
}

/**
 * Caixa de marcar nativa com o rótulo dentro do `label`: o alvo de toque é a linha toda, de no
 * mínimo 44 px. Caixa de 22 px e texto de 16 px no celular; 20 px e 14 px a partir do `md`.
 */
function Checkbox({ label, className, ...props }: CheckboxProps) {
  return (
    <label
      data-slot="checkbox"
      className={cn(
        "flex min-h-11 cursor-pointer items-center gap-3 text-body text-text md:gap-2.5 md:text-body-sm has-disabled:cursor-not-allowed has-disabled:text-text-muted",
        className,
      )}
    >
      <input
        type="checkbox"
        className="m-0 size-5.5 shrink-0 accent-brand md:size-5"
        {...props}
      />
      {label}
    </label>
  )
}

export { Checkbox }
