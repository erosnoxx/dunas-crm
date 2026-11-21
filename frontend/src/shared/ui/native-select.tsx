import * as React from "react"
import { ChevronDownIcon } from "lucide-react"

import { cn } from "@/shared/lib/cn"

import { FIELD_SIZES, type FieldSize } from "./field-size"

type NativeSelectProps = Omit<React.ComponentProps<"select">, "size"> & {
  size?: FieldSize
}

/**
 * Seleção nativa (teclado e leitor de tela de graça) sobre os mesmos tokens do `Input`. A seta é
 * um `ChevronDownIcon` por cima: o `appearance-none` tira a do navegador.
 */
function NativeSelect({
  className,
  size = "default",
  children,
  ...props
}: NativeSelectProps) {
  return (
    <div className="relative w-full min-w-0">
      <select
        data-slot="native-select"
        className={cn(
          "w-full min-w-0 appearance-none border border-line-strong bg-panel text-body text-text disabled:cursor-not-allowed disabled:border-line disabled:bg-nav disabled:text-text-muted aria-invalid:border-danger",
          FIELD_SIZES[size],
          // Depois do tamanho: o `px` dele apagaria o `pr` (o texto não passa por baixo da seta).
          "pr-10 aria-invalid:pr-10",
          className,
        )}
        {...props}
      >
        {children}
      </select>
      <ChevronDownIcon
        aria-hidden="true"
        className="pointer-events-none absolute top-1/2 right-3 size-5 -translate-y-1/2 text-text-muted"
      />
    </div>
  )
}

export { NativeSelect }
