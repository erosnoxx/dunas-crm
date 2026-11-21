import * as React from "react"

import { cn } from "@/shared/lib/cn"
import { Input } from "@/shared/ui/input"
import { Label } from "@/shared/ui/label"

import { PASSWORD_MAX } from "../password-rules"

type PasswordFieldProps = Omit<React.ComponentProps<"input">, "type" | "id"> & {
  id: string
  label: string
  error?: string
  /**
   * Mostra o contador "N de 30" ao lado do rótulo (só nas telas que definem senha). Exige `value`
   * controlado: sem ele o contador não acompanha o que a pessoa digita.
   */
  showCount?: boolean
}

/**
 * Campo de senha: `maxLength` de 30 (o navegador corta o que passar ao colar) e botão
 * Mostrar/Ocultar. O valor nunca é aparado nem registrado.
 */
function PasswordField({
  id,
  label,
  error,
  showCount = false,
  className,
  ...inputProps
}: PasswordFieldProps) {
  const [visible, setVisible] = React.useState(false)
  const errorId = `${id}-error`
  const count = String(inputProps.value ?? "").length
  const describedBy =
    [inputProps["aria-describedby"], error ? errorId : undefined]
      .filter(Boolean)
      .join(" ") || undefined

  return (
    <div className="flex flex-col gap-1.5">
      <div className="flex items-baseline justify-between">
        <Label htmlFor={id} className={cn(inputProps.disabled && "text-text-muted")}>
          {label}
        </Label>
        {showCount && (
          <span className="text-caption text-text-muted">
            {count} de {PASSWORD_MAX}
          </span>
        )}
      </div>
      <div className="relative flex">
        <Input
          aria-invalid={error ? true : undefined}
          {...inputProps}
          aria-describedby={describedBy}
          maxLength={PASSWORD_MAX}
          id={id}
          type={visible ? "text" : "password"}
          className={cn("pr-21", className)}
        />
        <button
          type="button"
          aria-pressed={visible}
          aria-controls={id}
          disabled={inputProps.disabled}
          onClick={() => setVisible((v) => !v)}
          className="absolute top-0 right-0 h-13 min-h-11 min-w-18 text-body-sm font-semibold text-brand-text disabled:cursor-not-allowed disabled:text-text-muted md:h-12"
        >
          {visible ? "Ocultar" : "Mostrar"}
        </button>
      </div>
      {error && (
        <p id={errorId} className="text-caption text-danger-text">
          {error}
        </p>
      )}
    </div>
  )
}

export { PasswordField }
