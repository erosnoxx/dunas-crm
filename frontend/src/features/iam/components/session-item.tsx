import { MonitorIcon, SmartphoneIcon } from "lucide-react"

import { Button } from "@/shared/ui/button"
import { StatusChip } from "@/shared/ui/status-chip"

import type { SessionSummary } from "../iam.service"

const MINUTE = 60_000
const HOUR = 60 * MINUTE
const DAY = 24 * HOUR

const relative = new Intl.RelativeTimeFormat("pt-BR", { numeric: "always" })
const shortDate = new Intl.DateTimeFormat("pt-BR", { day: "2-digit", month: "2-digit" })

/** "agora", "há 5 minutos", "há 3 horas", "há 2 dias". Relógio torto (futuro) conta como agora. */
function lastUse(iso: string, now: Date): string {
  const elapsed = now.getTime() - new Date(iso).getTime()

  if (elapsed < MINUTE) return "agora"
  if (elapsed < HOUR) return relative.format(-Math.floor(elapsed / MINUTE), "minute")
  if (elapsed < DAY) return relative.format(-Math.floor(elapsed / HOUR), "hour")

  return relative.format(-Math.floor(elapsed / DAY), "day")
}

// shortcut: a API só manda o rótulo livre do aparelho, sem tipo; tablets (iPad) e rótulos fora da lista são aproximação, troca por um campo `kind` se o backend passar a mandar.
const PHONE = /iphone|ipad|\bios\b|android|galaxy|\bapp\b/i

type SessionItemProps = {
  session: SessionSummary
  now: Date
  pending: boolean
  onRevoke: (id: string) => void
}

/** Uma sessão ativa (mockups "Minhas sessões"). A do aparelho é marcada e não tem "Encerrar". */
function SessionItem({ session, now, pending, onRevoke }: SessionItemProps) {
  const DeviceIcon = PHONE.test(session.device) ? SmartphoneIcon : MonitorIcon

  return (
    <li className="cut-lg flex flex-col items-start gap-3 bg-panel p-4">
      <div className="flex items-start gap-3">
        <DeviceIcon aria-hidden="true" className="mt-0.5 size-6 text-text-muted" />
        <div className="flex flex-col gap-0.5">
          <span className="text-body font-semibold">{session.device}</span>
          <span className="text-body-sm text-text-muted">
            {`Último uso: ${lastUse(session.lastUsedAt, now)}`}
          </span>
          <span className="text-caption text-text-muted">
            {`Entrou em ${shortDate.format(new Date(session.startedAt))}`}
          </span>
        </div>
      </div>
      {session.current ? (
        <StatusChip tone="ok">Esta sessão</StatusChip>
      ) : (
        <Button
          variant="outline"
          loading={pending}
          onClick={() => onRevoke(session.id)}
          className="text-sm md:[&>span]:px-4 [&>span]:bg-panel"
        >
          Encerrar esta sessão
        </Button>
      )}
    </li>
  )
}

export { SessionItem }
