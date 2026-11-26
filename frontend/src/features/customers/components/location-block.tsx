import { ChevronDownIcon, InfoIcon, PlusIcon } from "lucide-react"
import * as React from "react"
import { Link } from "react-router"

import { cn } from "@/shared/lib/cn"
import { MD_UP, useMediaQuery } from "@/shared/lib/use-media-query"
import { Button } from "@/shared/ui/button"

import { activeEquipmentText } from "../customers.messages"
import type { Address, LocationView } from "../customers.service"
import { formatZipCode } from "../zip-code"
import { CustomerStatusChip } from "./customer-status"
import { EquipmentCard } from "./equipment-card"
import { EquipmentTable } from "./equipment-table"

const NOTE =
  "Local desativado: não recebe equipamento novo e não se edita até ser reativado. Os equipamentos dele ficam como estão."

// O `.open` das telas 9 e 10: link de ação laranja, 14 px 600, alvo de 44 px.
const OPEN =
  "inline-flex min-h-11 items-center text-body-sm font-semibold text-brand-text"

const addressText = (address: Address) =>
  [
    [address.street, address.number, address.complement].filter(Boolean).join(", "),
    address.neighborhood,
    `${address.city}/${address.state}`,
    `CEP ${formatZipCode(address.zipCode)}`,
  ].join(" · ")

type LocationBlockProps = {
  customerId: string
  location: LocationView
  customerActive: boolean
  canEdit: boolean
  onReactivate: () => void
  reactivating: boolean
}

/**
 * Um local na ficha. Web: o cartão da tela 9, sempre aberto, com a tabela. Celular: a seção
 * recolhível da tela 10, aberta se o local está ativo e fechada se desativado (ADR-22), com os
 * cartões. "Editar local" só no local ativo; "Novo equipamento" também exige o cliente ativo
 * (ADR-30: o local ativo de cliente desativado continua editável); o desativado tem o aviso e
 * "Reativar local". Tudo de escrita só com `canEdit`.
 */
function LocationBlock({
  customerId,
  location,
  customerActive,
  canEdit,
  onReactivate,
  reactivating,
}: LocationBlockProps) {
  const isDesktop = useMediaQuery(MD_UP)
  const titleId = React.useId()
  const active = location.status === "ACTIVE"
  const path = `/clientes/${customerId}/locais/${location.id}`
  const canEditLocation = canEdit && active
  const canInstall = canEditLocation && customerActive
  const count = location.equipment.filter((item) => item.status === "ACTIVE").length

  const chip = <CustomerStatusChip status={location.status} />
  const info = (
    <>
      <p className="mt-0.5 text-body-sm text-text-muted">
        {addressText(location.address)}
      </p>
      <p className="mt-0.5 text-body-sm">
        <span className="font-display font-semibold">{count}</span>
        {activeEquipmentText(count).slice(String(count).length)}
      </p>
    </>
  )
  const note = (className: string) => (
    <p
      className={cn(
        "flex items-start gap-2 border-line bg-nav py-2.5 text-body-sm text-text-muted",
        className,
      )}
    >
      <InfoIcon
        aria-hidden="true"
        strokeWidth={2.5}
        className="mt-0.5 size-4.5 shrink-0"
      />
      {NOTE}
    </p>
  )
  const newEquipment = (className: string, icon: string) => (
    <Link to={`${path}/equipamentos/novo`} className={className}>
      <PlusIcon aria-hidden="true" strokeWidth={2.5} className={icon} />
      Novo equipamento
    </Link>
  )

  if (isDesktop)
    return (
      <section aria-labelledby={titleId} className="border border-line bg-panel">
        <div className="flex flex-wrap items-start justify-between gap-4 border-line px-5 py-4 not-last:border-b">
          <div>
            <h3
              id={titleId}
              className={cn(
                "flex items-center gap-3 font-display text-xl leading-7 font-semibold",
                !active && "text-text-muted",
              )}
            >
              {location.name} {chip}
            </h3>
            {info}
          </div>
          <div className="flex shrink-0 items-center gap-4">
            {canEditLocation && (
              <Link to={path} className={OPEN}>
                Editar local
              </Link>
            )}
            {canInstall &&
              newEquipment(
                "cut-sm inline-flex min-h-11 items-center gap-2 bg-chip-ok px-5 text-body-sm font-semibold text-text",
                "size-4.5",
              )}
            {canEdit && !active && (
              <Button
                variant="link"
                loading={reactivating}
                onClick={onReactivate}
                className="text-body-sm no-underline"
              >
                Reativar local
              </Button>
            )}
          </div>
        </div>
        {!active && note("px-5 not-last:border-b")}
        {location.equipment.length > 0 && (
          <EquipmentTable
            equipment={location.equipment}
            canEdit={canEdit}
            customerId={customerId}
          />
        )}
      </section>
    )

  return (
    <details open={active} className="group border border-line bg-panel">
      <summary className="flex min-h-11 cursor-pointer list-none items-start gap-3 px-4 py-3.5 [&::-webkit-details-marker]:hidden">
        <div className="min-w-0 flex-1">
          <h3
            className={cn(
              "flex flex-wrap items-center gap-2 text-body font-semibold",
              !active && "text-text-muted",
            )}
          >
            {location.name} {chip}
          </h3>
          {info}
        </div>
        <ChevronDownIcon
          aria-hidden="true"
          strokeWidth={2.5}
          className="mt-0.5 size-5 shrink-0 text-text-muted group-open:rotate-180"
        />
      </summary>
      {!active && note("border-t px-4")}
      {location.equipment.length > 0 && (
        <ul className="divide-y divide-line border-t border-line">
          {location.equipment.map((item) => (
            <li key={item.id}>
              <EquipmentCard
                item={item}
                href={
                  canEdit ? `/clientes/${customerId}/equipamentos/${item.id}` : undefined
                }
              />
            </li>
          ))}
        </ul>
      )}
      {canEditLocation && (
        <div className="flex items-center justify-between border-t border-line px-4 py-1">
          {canInstall && newEquipment(cn(OPEN, "gap-1.5"), "size-4")}
          <Link to={path} className={cn(OPEN, "ml-auto")}>
            Editar local
          </Link>
        </div>
      )}
      {canEdit && !active && (
        <div className="border-t border-line px-4 py-3">
          <Button
            size="lg"
            loading={reactivating}
            onClick={onReactivate}
            className="min-h-12 w-full bg-chip-ok text-text"
          >
            Reativar local
          </Button>
        </div>
      )}
    </details>
  )
}

export { LocationBlock }
