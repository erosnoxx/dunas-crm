import type { CustomerSummary } from "../customers.service"

/** D1 (ADR-27): uma linha "Série X · modelo" por série casada, no estilo da linha do documento da tela 2. */
function MatchedSerials({ items }: { items: CustomerSummary["matchedSerials"] }) {
  return items.map(({ serialNumber, modelName }) => (
    <span key={serialNumber} className="block text-body-sm text-text-muted">
      Série {serialNumber} · {modelName}
    </span>
  ))
}

export { MatchedSerials }
