import * as React from "react"
import { Input as InputPrimitive } from "@base-ui/react/input"

import { cn } from "@/shared/lib/cn"

/** Campo de texto: 52 px no celular e 48 px a partir do `md`; `maxLength` vem do domínio. */
function Input({ className, ...props }: React.ComponentProps<"input">) {
  return (
    <InputPrimitive
      data-slot="input"
      className={cn(
        "h-13 w-full min-w-0 border border-line-strong bg-panel px-3.5 text-body text-text placeholder:text-text-muted disabled:cursor-not-allowed disabled:border-line disabled:text-text-muted aria-invalid:border-danger md:h-12",
        className,
      )}
      {...props}
    />
  )
}

export { Input }
