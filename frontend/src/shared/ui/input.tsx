import * as React from "react"
import { Input as InputPrimitive } from "@base-ui/react/input"

import { cn } from "@/shared/lib/cn"

import { FIELD_SIZES, type FieldSize } from "./field-size"

type InputProps = Omit<React.ComponentProps<"input">, "size"> & { size?: FieldSize }

/** Campo de texto: `maxLength` vem do domínio; os tamanhos estão em `field-size.ts`. */
function Input({ className, size = "default", ...props }: InputProps) {
  return (
    <InputPrimitive
      data-slot="input"
      className={cn(
        "w-full min-w-0 border border-line-strong bg-panel text-body text-text placeholder:text-text-muted disabled:cursor-not-allowed disabled:border-line disabled:text-text-muted aria-invalid:border-danger",
        FIELD_SIZES[size],
        className,
      )}
      {...props}
    />
  )
}

export { Input }
