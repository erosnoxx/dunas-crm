import { Link } from "react-router"

import { cn } from "@/shared/lib/cn"

import { countText } from "../catalog.messages"
import type { ManufacturerSummary } from "../catalog.service"
import { CatalogStatusChip } from "./catalog-status"

// As colunas da tabela na web (mockup 7): a linha e o cabeçalho usam a mesma grade.
const COLUMNS = "md:grid-cols-[2fr_1fr_1.4fr_auto]"

/** O cabeçalho da tabela (só na web): `th` do mockup 7, `label` em `text-muted`. */
function ManufacturerTableHeader() {
  return (
    <div
      aria-hidden="true"
      className={cn(
        "grid gap-4 border-b border-line px-4 py-3 text-label text-text-muted max-md:hidden",
        COLUMNS,
      )}
    >
      <span>Fabricante</span>
      <span>Modelos</span>
      <span>Situação</span>
      <span />
    </div>
  )
}

/**
 * Um fabricante da lista (mockups 7 e 8). O mesmo elemento é a linha da tabela na web (nome,
 * número, situação e "Editar") e o cartão do celular (nome e "3 modelos" à esquerda, chip à
 * direita), por `md:contents`, como a `ModelRow`. Quem edita abre o painel pelo link; quem só
 * consulta (ADR-6) vê a linha sem link, porque não há o que abrir.
 */
function ManufacturerRow({
  manufacturer,
  canEdit,
}: {
  manufacturer: ManufacturerSummary
  canEdit: boolean
}) {
  const content = (
    <span
      className={cn(
        "flex min-h-16 items-center justify-between gap-2 border border-line bg-panel px-4 py-3.5 md:grid md:min-h-14 md:gap-4 md:border-0 md:border-b md:bg-transparent md:py-0 md:text-body-sm",
        COLUMNS,
      )}
    >
      <span className="flex flex-col md:contents">
        <span className="text-body font-semibold md:text-body-sm">
          {manufacturer.name}
        </span>
        <span className="text-body-sm text-text-muted md:hidden">
          {countText(manufacturer.modelCount)}
        </span>
        <span className="text-number tabular-nums max-md:hidden">
          {manufacturer.modelCount}
        </span>
      </span>
      <CatalogStatusChip status={manufacturer.status} />
      {canEdit && (
        <span className="text-body-sm font-semibold text-brand-text max-md:hidden md:text-end">
          Editar
        </span>
      )}
    </span>
  )

  return (
    <li>
      {canEdit ? (
        <Link to={`/catalogo/fabricantes/${manufacturer.id}`} className="block">
          {content}
        </Link>
      ) : (
        content
      )}
    </li>
  )
}

export { ManufacturerRow, ManufacturerTableHeader }
