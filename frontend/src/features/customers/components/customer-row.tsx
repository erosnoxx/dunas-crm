import * as React from "react"
import { Link } from "react-router"

import { cn } from "@/shared/lib/cn"

import { listCounts } from "../customers.messages"
import type { CustomerSummary } from "../customers.service"
import { CityList } from "./city-list"
import { CustomerStatusChip } from "./customer-status"
import { DocumentText } from "./document-text"
import { MatchedSerials } from "./matched-serials"

type Column = {
  header: React.ReactNode
  /** Só do `td`: o `th` é o mesmo para todas (`th` da tela 1, Main.dc.html:35). */
  cellClassName?: string
  cell: (customer: CustomerSummary, text: string) => React.ReactNode
}

// Os números das contagens (Main.dc.html:43): Chakra Petch 600, algarismos tabulares, no 14 do `td`.
const COUNT = "font-display font-semibold tabular-nums"

// A única definição das colunas: o cabeçalho e as linhas saem dela (decisão da Tarefa 1).
const COLUMNS: Column[] = [
  {
    header: "Cliente",
    cell: (customer) => (
      <>
        <span className="font-semibold">{customer.name}</span>
        {customer.tradeName && (
          <span className="block text-label font-normal text-text-muted">
            {customer.tradeName}
          </span>
        )}
        <MatchedSerials items={customer.matchedSerials} />
      </>
    ),
  },
  {
    header: "Documento",
    // Main.dc.html:40: o documento não quebra.
    cellClassName: "whitespace-nowrap tabular-nums",
    cell: (customer) => (
      <DocumentText type={customer.documentType} value={customer.document} />
    ),
  },
  {
    header: "Cidades",
    cell: (customer, text) => <CityList cities={customer.cities} text={text} />,
  },
  { header: "Locais", cellClassName: COUNT, cell: (customer) => customer.locationCount },
  {
    header: "Equipamentos",
    cellClassName: COUNT,
    cell: (customer) => customer.equipmentCount,
  },
  {
    header: "Situação",
    cell: (customer) => <CustomerStatusChip status={customer.status} />,
  },
  {
    header: <span className="sr-only">Ações</span>,
    cellClassName: "text-right",
    cell: (customer) => (
      <Link
        to={`/clientes/${customer.id}`}
        // Alvo de 44 px dentro do `td` de 48 (Main.dc.html:36): a margem negativa come o respiro de 8.
        className="-my-1.5 inline-flex min-h-11 items-center font-semibold text-brand-text"
      >
        Abrir
      </Link>
    ),
  },
]

/**
 * Os clientes na web (tela 1): `th` 13/18 600 `text-muted` com 12/16 de respiro; `td` de 48 px,
 * 14/20, 8/16 de respiro, linha `line` entre as linhas e nenhuma na última (Main.dc.html:35-37).
 */
function CustomerTable({
  customers,
  text,
}: {
  customers: CustomerSummary[]
  text: string
}) {
  return (
    <table className="w-full border-collapse text-left">
      <thead>
        <tr>
          {COLUMNS.map((column, index) => (
            <th
              key={index}
              className="border-b border-line px-4 py-3 text-label text-text-muted"
            >
              {column.header}
            </th>
          ))}
        </tr>
      </thead>
      <tbody className="text-body-sm">
        {customers.map((customer) => (
          <tr key={customer.id} className="border-b border-line last:border-b-0">
            {COLUMNS.map((column, index) => (
              <td key={index} className={cn("h-12 px-4 py-2", column.cellClassName)}>
                {column.cell(customer, text)}
              </td>
            ))}
          </tr>
        ))}
      </tbody>
    </table>
  )
}

/**
 * Um cliente no celular (telas 2 e 5, Clientes-celular.dc.html:28-35): o cartão inteiro é o link;
 * nome 16 600, apoio 14/20 `text-muted`, a série casada abaixo do nome (D1) e, embaixo, as
 * contagens em Chakra Petch 16 600 e o selo.
 */
function CustomerCard({ customer, text }: { customer: CustomerSummary; text: string }) {
  return (
    <li>
      <Link
        to={`/clientes/${customer.id}`}
        className="block min-h-11 border border-line bg-panel px-4 py-3.5"
      >
        <span className="block text-body font-semibold">{customer.name}</span>
        <span className="block text-body-sm text-text-muted">
          {customer.tradeName && <span className="block">{customer.tradeName}</span>}
          <MatchedSerials items={customer.matchedSerials} />
          <span className="block tabular-nums">
            <DocumentText type={customer.documentType} value={customer.document} />
          </span>
          <span className="block">
            <CityList cities={customer.cities} text={text} />
          </span>
        </span>
        <span className="mt-2 flex items-center justify-between gap-2">
          <span className="text-number tabular-nums">
            {listCounts(customer.locationCount, customer.equipmentCount)}
          </span>
          <CustomerStatusChip status={customer.status} />
        </span>
      </Link>
    </li>
  )
}

export { CustomerCard, CustomerTable }
