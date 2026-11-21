import { Link } from "react-router"

import { cn } from "@/shared/lib/cn"

import { amountText } from "../amount"
import type { EquipmentModelSummary } from "../catalog.service"
import { CatalogStatusChip } from "./catalog-status"

// As colunas da tabela na web: a linha e o cabeçalho usam a mesma grade, e o cabeçalho leva o
// "Abrir" invisível (sem altura, para não mudar a do cabeçalho), para a coluna `auto` (e as
// frações) terem a mesma largura nos dois.
const COLUMNS = "md:grid-cols-[1fr_1.1fr_1.6fr_1.4fr_1fr_auto]"
const OPEN = "text-body-sm font-semibold"

/** O cabeçalho da tabela (só na web): `th` do mockup 1, `label` em `text-muted`. */
function ModelTableHeader() {
  return (
    <div
      aria-hidden="true"
      className={cn(
        "grid gap-4 border-b border-line px-4 py-3 text-label text-text-muted max-md:hidden",
        COLUMNS,
      )}
    >
      <span>Fabricante</span>
      <span>Tipo</span>
      <span>Modelo</span>
      <span>Característica</span>
      <span>Situação</span>
      <span className={cn(OPEN, "invisible h-0")}>Abrir</span>
    </div>
  )
}

/**
 * Um modelo da lista (mockups "Modelos, web" e "Modelos, celular"). O mesmo elemento é a linha da
 * tabela na web (seis colunas, com "Abrir") e o cartão do celular (nome, "fabricante · tipo", a
 * característica e o chip), por grade, `order` e `md:contents`, como a `UserRow` do IAM. É um
 * link para `/catalogo/modelos/:id`, que abre só leitura para quem não edita.
 */
function ModelRow({ model }: { model: EquipmentModelSummary }) {
  const value = model.values[0]

  return (
    <li>
      <Link to={`/catalogo/modelos/${model.id}`} className="group block">
        <span
          className={cn(
            "grid grid-cols-[1fr_auto] items-center gap-x-2 border border-line bg-panel px-4 py-3.5 md:min-h-14 md:gap-4 md:border-0 md:border-b md:bg-transparent md:py-0 md:text-body-sm",
            COLUMNS,
          )}
        >
          <span className="col-span-2 text-body font-semibold md:order-3 md:col-span-1 md:text-body-sm md:font-semibold">
            {model.name}
          </span>
          <span className="col-span-2 text-body-sm text-text-muted md:contents md:text-text">
            <span className="md:order-1">{model.manufacturer.name}</span>
            <span className="md:hidden"> · </span>
            <span className="md:order-2">{model.type.name}</span>
          </span>
          <span className="mt-2 text-number tabular-nums md:order-4 md:mt-0">
            {value && `${value.name} ${amountText(value)}`}
          </span>
          <span className="mt-2 justify-self-end md:order-5 md:mt-0 md:justify-self-start">
            <CatalogStatusChip status={model.status} />
          </span>
          <span className={cn(OPEN, "text-brand-text max-md:hidden md:order-6")}>
            Abrir
          </span>
        </span>
      </Link>
    </li>
  )
}

export { ModelRow, ModelTableHeader }
