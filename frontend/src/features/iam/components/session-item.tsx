import { MonitorIcon, SmartphoneIcon } from "lucide-react"

import { cn } from "@/shared/lib/cn"
import { Button } from "@/shared/ui/button"
import { StatusChip } from "@/shared/ui/status-chip"

import type { SessionSummary } from "../iam.service"
import { lastUse } from "../last-use"

const shortDate = new Intl.DateTimeFormat("pt-BR", { day: "2-digit", month: "2-digit" })

// shortcut: a API só manda o rótulo livre do aparelho, sem tipo; tablets (iPad) e rótulos fora da lista são aproximação, troca por um campo `kind` se o backend passar a mandar.
const PHONE = /iphone|ipad|\bios\b|android|galaxy|\bapp\b/i

/**
 * `card`: cartão próprio (Minhas sessões). `stacked`: dentro do painel, com a marca e o botão
 * abaixo (a lista do próprio usuário). `inline`: dentro do painel, com "Encerrar" ao lado (a
 * lista de outro usuário).
 */
type Layout = "card" | "stacked" | "inline"

type SessionItemProps = {
  session: SessionSummary
  now: Date
  pending: boolean
  onRevoke: (id: string) => void
  layout?: Layout
}

/** Uma sessão ativa (mockups "Minhas sessões" e do painel do usuário). A do aparelho é marcada e não tem "Encerrar". */
function SessionItem({
  session,
  now,
  pending,
  onRevoke,
  layout = "card",
}: SessionItemProps) {
  const DeviceIcon = PHONE.test(session.device) ? SmartphoneIcon : MonitorIcon
  const icon = (inline: boolean) => (
    <DeviceIcon
      aria-hidden="true"
      className={cn("size-6 shrink-0 text-text-muted", !inline && "mt-0.5")}
    />
  )

  if (layout === "inline")
    return (
      <li className="flex items-center gap-3 border-b border-line pb-3">
        {icon(true)}
        <div className="flex min-w-0 flex-1 flex-col">
          <span className="text-body-sm font-semibold">{session.device}</span>
          <span className="text-label font-normal text-text-muted">
            {`Último uso: ${lastUse(session.lastUsedAt, now)}`}
          </span>
        </div>
        <Button
          variant="outline"
          loading={pending}
          // O texto visível é "Encerrar"; o nome acessível começa por ele e diz de qual aparelho.
          aria-label={`Encerrar, ${session.device}`}
          onClick={() => !pending && onRevoke(session.id)}
          className="text-body-sm [&>span]:bg-panel [&>span]:px-4"
        >
          Encerrar
        </Button>
      </li>
    )

  return (
    <li
      className={cn(
        "flex flex-col items-start gap-3",
        layout === "card" ? "cut-lg bg-panel p-4" : "border-b border-line pb-3",
      )}
    >
      <div className="flex items-start gap-3">
        {icon(false)}
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
          onClick={() => !pending && onRevoke(session.id)}
          className="text-body-sm md:[&>span]:px-4 [&>span]:bg-panel"
        >
          Encerrar esta sessão
        </Button>
      )}
    </li>
  )
}

export { SessionItem }
