import { StatusChip } from "@/shared/ui/status-chip"

import { LABELS } from "../customers.messages"
import type { CustomerStatus } from "../customers.service"

/** Situação por forma, como o `CatalogStatusChip` (decisão da Tarefa 1): cheio no ativo, vazado no desativado. */
function CustomerStatusChip({ status }: { status: CustomerStatus }) {
  return status === "ACTIVE" ? (
    <StatusChip tone="ok">{LABELS.status.ACTIVE}</StatusChip>
  ) : (
    <StatusChip tone="off" shape="ring">
      {LABELS.status.DEACTIVATED}
    </StatusChip>
  )
}

export { CustomerStatusChip }
