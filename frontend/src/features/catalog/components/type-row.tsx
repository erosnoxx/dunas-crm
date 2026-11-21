import { Link } from "react-router"

import { countText, LABELS } from "../catalog.messages"
import type { EquipmentTypeSummary } from "../catalog.service"
import { CatalogStatusChip } from "./catalog-status"

const COLUMNS = ["Tipo", "Característica", "Unidade", "Modelos", "Situação"]
const CELL = "h-14 border-b border-line px-3.5"

/** A característica do tipo (na primeira versão, uma só) e a unidade escrita como na tela. */
function characteristicOf(type: EquipmentTypeSummary) {
  const characteristic = type.characteristics[0]

  return {
    name: characteristic?.name ?? "",
    unit: characteristic ? LABELS.unit[characteristic.unit] : "",
  }
}

/**
 * A tabela da web (mockup 9): colunas de largura automática, como o `table` do mockup. "Editar"
 * só para quem edita (ADR-6); a linha do tipo aberto no painel traz "Editando".
 */
function TypeTable({
  types,
  canEdit,
  selectedId,
}: {
  types: EquipmentTypeSummary[]
  canEdit: boolean
  selectedId: string | undefined
}) {
  return (
    <table className="w-full border-collapse text-left">
      <thead>
        <tr>
          {COLUMNS.map((column) => (
            <th
              key={column}
              className="border-b border-line px-3.5 py-3 text-label text-text-muted"
            >
              {column}
            </th>
          ))}
          <th className="border-b border-line px-3.5 py-3">
            <span className="sr-only">Ações</span>
          </th>
        </tr>
      </thead>
      <tbody className="text-body-sm [&>tr:last-child>td]:border-b-0">
        {types.map((type) => {
          const { name, unit } = characteristicOf(type)
          const selected = type.id === selectedId

          return (
            <tr key={type.id}>
              <td className={`${CELL} font-semibold`}>{type.name}</td>
              <td className={CELL}>{name}</td>
              <td className={`${CELL} font-display font-semibold tabular-nums`}>
                {unit}
              </td>
              <td className={`${CELL} font-display font-semibold tabular-nums`}>
                {type.modelCount}
              </td>
              <td className={CELL}>
                <CatalogStatusChip status={type.status} />
              </td>
              <td className={`${CELL} text-right`}>
                {canEdit && (
                  <Link
                    to={`/catalogo/tipos/${type.id}`}
                    aria-current={selected ? "true" : undefined}
                    className="font-semibold text-brand-text"
                  >
                    {selected ? "Editando" : "Editar"}
                  </Link>
                )}
              </td>
            </tr>
          )
        })}
      </tbody>
    </table>
  )
}

/**
 * Os cartões do celular (mockup 10): tipo, "Característica: Potência (kW)", o número de modelos e
 * o chip. Quem edita abre o tipo pelo link; quem só consulta (ADR-6) vê o cartão sem link.
 */
function TypeCards({
  types,
  canEdit,
}: {
  types: EquipmentTypeSummary[]
  canEdit: boolean
}) {
  return (
    <ul className="flex flex-col gap-3">
      {types.map((type) => {
        const { name, unit } = characteristicOf(type)
        const card = (
          <span className="block min-h-11 border border-line bg-panel px-4 py-3.5">
            <span className="block text-body font-semibold">{type.name}</span>
            <span className="block text-body-sm text-text-muted">
              Característica: {name} ({unit})
            </span>
            <span className="mt-2 flex items-center justify-between gap-2">
              <span className="text-number tabular-nums">
                {countText(type.modelCount)}
              </span>
              <CatalogStatusChip status={type.status} />
            </span>
          </span>
        )

        return (
          <li key={type.id}>
            {canEdit ? (
              <Link to={`/catalogo/tipos/${type.id}`} className="block">
                {card}
              </Link>
            ) : (
              card
            )}
          </li>
        )
      })}
    </ul>
  )
}

export { TypeCards, TypeTable }
