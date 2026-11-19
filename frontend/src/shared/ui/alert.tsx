import * as React from "react"
import { AlertCircleIcon, ClockIcon } from "lucide-react"

import { cn } from "@/shared/lib/cn"

type AlertProps = {
  tone: "danger" | "warning"
  title?: string
  children: React.ReactNode
  className?: string
}

/**
 * Aviso na tela. `danger` é contorno com texto `danger-text` (erro que a pessoa corrige);
 * `warning` é o painel `highlight` com texto escuro (algo a esperar, como o acesso pausado).
 */
function Alert({ tone, title, children, className }: AlertProps) {
  const Icon = tone === "danger" ? AlertCircleIcon : ClockIcon

  return (
    <div
      role="alert"
      className={cn(
        "flex items-start gap-3",
        tone === "danger"
          ? "border border-danger p-3 text-body-sm text-danger-text"
          : "cut-lg bg-highlight p-4 text-body-sm text-on-highlight",
        className,
      )}
    >
      <Icon
        aria-hidden="true"
        className={cn(
          "mt-0.5 shrink-0",
          tone === "danger" ? "size-5 text-danger" : "size-6",
        )}
      />
      <div className="flex flex-col gap-1">
        {title && <p className="text-body font-semibold">{title}</p>}
        <p>{children}</p>
      </div>
    </div>
  )
}

export { Alert }
