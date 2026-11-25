import type { DocumentType } from "../customers.service"
import { formatDocument } from "../document"

/**
 * O tipo à parte e o número formatado. Na lista (telas 1 e 2): no celular a linha "CPF 000..." na
 * cor do pai; na web o tipo em `caption` 600 `text-muted`, 6 px antes do número. Com `badge` (ficha,
 * tela 9): o selo `chip-ok` do tipo, 8 px antes do número.
 */
function DocumentText({
  type,
  value,
  badge = false,
}: {
  type: DocumentType
  value: string
  badge?: boolean
}) {
  return (
    <span className="whitespace-nowrap">
      {badge ? (
        <span className="mr-1.5 bg-chip-ok px-2 py-0.5 text-caption font-semibold">
          {type}
        </span>
      ) : (
        <span className="md:mr-1 md:text-caption md:font-semibold md:text-text-muted">
          {type}
        </span>
      )}{" "}
      <span>{formatDocument(value)}</span>
    </span>
  )
}

export { DocumentText }
