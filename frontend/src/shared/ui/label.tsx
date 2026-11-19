import * as React from "react"

import { cn } from "@/shared/lib/cn"

function Label({ className, ...props }: React.ComponentProps<"label">) {
  return (
    <label
      data-slot="label"
      className={cn(
        "text-label text-text select-none peer-disabled:opacity-50",
        className,
      )}
      {...props}
    />
  )
}

export { Label }
