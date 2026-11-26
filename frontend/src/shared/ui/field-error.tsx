import * as React from "react"
import { CircleAlertIcon } from "lucide-react"

/** Erro sob o campo, como no mockup 4 do catálogo: ícone e texto `body-sm` em `danger-text`, não só cor. */
function FieldError({ id, children }: { id: string; children: React.ReactNode }) {
  return (
    <p id={id} className="flex items-start gap-1.5 text-body-sm text-danger-text">
      <CircleAlertIcon
        aria-hidden="true"
        className="mt-0.5 size-4.5 shrink-0 text-danger"
      />
      {children}
    </p>
  )
}

export { FieldError }
