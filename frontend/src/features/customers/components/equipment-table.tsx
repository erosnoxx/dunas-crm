import * as React from "react"
import { Link } from "react-router"

import { cn } from "@/shared/lib/cn"

import type { EquipmentView } from "../customers.service"
import { formatDate } from "../display"
import { CustomerStatusChip } from "./customer-status"
import { TypeCharacteristic } from "./equipment-card"

type Column = {
  header: React.ReactNode
  /** Só do `td`: o `th` é o mesmo para todas (o `th` do mockup). */
  cellClassName?: string
  cell: (item: EquipmentView, href: string | undefined) => React.ReactNode
}

// A única definição das colunas: o cabeçalho e as linhas saem dela (decisão da Tarefa 1).
const COLUMNS: Column[] = [
  { header: "Modelo", cellClassName: "font-semibold", cell: (item) => item.model.name },
  { header: "Fabricante", cell: (item) => item.manufacturer.name },
  {
    header: "Tipo e característica",
    cell: (item) => <TypeCharacteristic item={item} />,
  },
  {
    header: "Número de série",
    cellClassName: "tracking-[0.02em]",
    cell: (item) => item.serialNumber,
  },
  {
    header: "Instalado em",
    cell: (item) =>
      item.installedOn ? (
        <span className="font-display font-semibold">{formatDate(item.installedOn)}</span>
      ) : (
        <span className="text-text-muted">Sem data</span>
      ),
  },
  { header: "Situação", cell: (item) => <CustomerStatusChip status={item.status} /> },
  {
    header: <span className="sr-only">Ações</span>,
    cellClassName: "text-right",
    cell: (item, href) =>
      href && (
        <Link
          to={href}
          className="inline-flex min-h-11 items-center font-semibold text-brand-text"
        >
          {item.status === "ACTIVE" ? "Editar" : "Abrir"}
        </Link>
      ),
  },
]

/**
 * Os equipamentos de um local na web (tela 9). Quem edita abre o equipamento: "Editar" no ativo,
 * "Abrir" no desativado; o Técnico não vê a coluna preenchida. A linha desativada fica em
 * `text-muted`. A guarda rola a tabela na largura estreita em vez de estourar a página.
 */
function EquipmentTable({
  equipment,
  canEdit,
  customerId,
}: {
  equipment: EquipmentView[]
  canEdit: boolean
  customerId: string
}) {
  return (
    // `relative`: o "Ações" `sr-only` (absoluto) do cabeçalho rola junto, sem alargar a página.
    <div className="relative overflow-x-auto">
      <table className="w-full border-collapse text-left">
        <thead>
          <tr>
            {COLUMNS.map((column, index) => (
              <th
                key={index}
                className="border-b border-line px-2 py-3 text-label xl:px-4 text-text-muted"
              >
                {column.header}
              </th>
            ))}
          </tr>
        </thead>
        <tbody className="text-body-sm">
          {equipment.map((item) => {
            const href = canEdit
              ? `/clientes/${customerId}/equipamentos/${item.id}`
              : undefined

            return (
              <tr
                key={item.id}
                className={cn(
                  "border-b border-line last:border-b-0",
                  item.status === "DEACTIVATED" && "text-text-muted",
                )}
              >
                {COLUMNS.map((column, index) => (
                  <td
                    key={index}
                    className={cn("h-14 px-2 xl:px-4", column.cellClassName)}
                  >
                    {column.cell(item, href)}
                  </td>
                ))}
              </tr>
            )
          })}
        </tbody>
      </table>
    </div>
  )
}

export { EquipmentTable }
