import * as React from "react"
import { CircleAlertIcon } from "lucide-react"

import { Input } from "@/shared/ui/input"
import { Label } from "@/shared/ui/label"

import { LABELS } from "../catalog.messages"
import type { EquipmentTypeSummary } from "../catalog.service"

type ModelValuesFieldsProps = {
  /** O tipo escolhido; sem ele não há o que preencher e nada é desenhado. */
  type: EquipmentTypeSummary | undefined
  value: string
  onChange: (value: string) => void
  error?: string
}

/**
 * A seção "Características do tipo ..." do formulário do modelo. O formulário guarda um único
 * valor (`amount`) porque cada tipo tem uma característica (domain-model.md), então só ela é
 * desenhada. O valor é do pai; trocar o tipo chama `onChange("")` (efeito, que não roda na
 * montagem nem quando o tipo chega depois dela, para não apagar o valor de um modelo em edição), e nada digitado vaza para o tipo novo.
 */
function ModelValuesFields({ type, value, onChange, error }: ModelValuesFieldsProps) {
  const id = React.useId()
  const typeId = type?.id
  const previousTypeId = React.useRef(typeId)

  React.useEffect(() => {
    if (previousTypeId.current === typeId) return

    const wasUnset = previousTypeId.current === undefined
    previousTypeId.current = typeId
    // Do "sem tipo" para o tipo que carregou não há o que limpar: o valor é o da edição.
    if (!wasUnset) onChange("")
  }, [typeId, onChange])

  const characteristic = type?.characteristics[0]

  if (!type || !characteristic) return null

  const fieldId = `${id}-amount`
  const hintId = `${id}-hint`
  const errorId = `${id}-error`

  return (
    <section
      aria-labelledby={`${id}-title`}
      className="flex flex-col gap-3 border-t border-line pt-4.5 md:pt-5"
    >
      <h2 id={`${id}-title`} className="text-label text-text-muted">
        Características do tipo {type.name}
      </h2>
      <div className="flex flex-col gap-1.5">
        <Label htmlFor={fieldId}>
          {characteristic.name}
          {characteristic.required ? " *" : ""}
        </Label>
        <div className="flex items-stretch">
          <Input
            id={fieldId}
            size="compact"
            inputMode="decimal"
            value={value}
            onChange={(event) => onChange(event.target.value)}
            aria-invalid={error ? true : undefined}
            aria-describedby={[hintId, error ? errorId : undefined]
              .filter(Boolean)
              .join(" ")}
            className="flex-1"
          />
          <span className="flex items-center border border-l-0 border-line-strong bg-chip-ok px-3.5 text-body-sm font-semibold text-text">
            {LABELS.unit[characteristic.unit]}
          </span>
        </div>
        {error && (
          <p
            id={errorId}
            className="flex items-start gap-1.5 text-body-sm text-danger-text"
          >
            <CircleAlertIcon
              aria-hidden="true"
              className="mt-0.5 size-4.5 shrink-0 text-danger"
            />
            {error}
          </p>
        )}
        <p id={hintId} className="hidden text-caption text-text-muted md:block">
          O campo e a unidade vêm do tipo escolhido. Ao trocar o tipo, o campo muda.
        </p>
      </div>
    </section>
  )
}

export { ModelValuesFields }
