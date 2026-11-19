import * as React from "react"

import { cn } from "@/shared/lib/cn"

/** Painel principal: superfície `panel` com o corte `cut-lg` do logo. */
function Panel({ className, ...props }: React.ComponentProps<"section">) {
  return (
    <section data-slot="panel" className={cn("cut-lg bg-panel", className)} {...props} />
  )
}

export { Panel }
