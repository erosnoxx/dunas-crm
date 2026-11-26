import { Link } from "react-router"

import { cn } from "@/shared/lib/cn"

import { LABELS } from "../customers.messages"
import type { EquipmentView } from "../customers.service"
import { formatAmount, formatDate } from "../display"
import { CustomerStatusChip } from "./customer-status"

/** "Inversor · 5 kW", com o valor em `font-display` 600 (o `.num` das telas 9 e 10). */
function TypeCharacteristic({ item }: { item: EquipmentView }) {
  const { characteristic } = item

  return (
    <>
      {item.type.name}
      {characteristic && (
        <>
          {" · "}
          <span className="font-display font-semibold">
            {formatAmount(characteristic.amount)} {LABELS.unit[characteristic.unit]}
          </span>
        </>
      )}
    </>
  )
}

/**
 * Um equipamento no local do celular (telas 10 e 16): modelo, "fabricante · tipo · valor",
 * "Série X · data" e o selo. Com `href` (quem edita) o cartão inteiro é o link; sem ele (Técnico),
 * só texto.
 */
function EquipmentCard({ item, href }: { item: EquipmentView; href?: string }) {
  const content = (
    <>
      <span className="min-w-0 flex-1">
        <span
          className={cn(
            "block text-body font-semibold",
            item.status === "DEACTIVATED" && "text-text-muted",
          )}
        >
          {item.model.name}
        </span>
        <span className="block text-body-sm text-text-muted">
          {item.manufacturer.name} · <TypeCharacteristic item={item} />
        </span>
        <span className="block text-body-sm text-text-muted">
          Série {item.serialNumber} ·{" "}
          {item.installedOn ? formatDate(item.installedOn) : "sem data"}
        </span>
      </span>
      <CustomerStatusChip status={item.status} />
    </>
  )
  const card = "flex items-start gap-2.5 px-4 py-2.5"

  return href ? (
    <Link to={href} className={card}>
      {content}
    </Link>
  ) : (
    <div className={card}>{content}</div>
  )
}

export { EquipmentCard, TypeCharacteristic }
