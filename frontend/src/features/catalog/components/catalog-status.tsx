import { StatusChip } from "@/shared/ui/status-chip"

import { LABELS } from "../catalog.messages"
import type { CatalogStatus } from "../catalog.service"

/** Situação por forma (mockups 1, 2, 7 e 9): círculo cheio no ativo, vazado e apagado no desativado. */
function CatalogStatusChip({ status }: { status: CatalogStatus }) {
  return status === "ACTIVE" ? (
    <StatusChip tone="ok">{LABELS.status.ACTIVE}</StatusChip>
  ) : (
    <StatusChip tone="off" shape="ring">
      {LABELS.status.DEACTIVATED}
    </StatusChip>
  )
}

export { CatalogStatusChip }
