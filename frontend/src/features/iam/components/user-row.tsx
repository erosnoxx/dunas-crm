import { NavLink } from "react-router"

import { StatusChip } from "@/shared/ui/status-chip"

import { LABELS } from "../iam.messages"
import type { UserStatus, UserSummary } from "../iam.service"
import { lastUse } from "../last-use"

const TONE: Record<UserStatus, "ok" | "pending" | "off"> = {
  ACTIVE: "ok",
  INVITED: "pending",
  DEACTIVATED: "off",
}

type UserRowProps = { user: UserSummary; isMe: boolean; now: Date }

/**
 * Uma linha da lista (mockups "Usuários · web" e "Usuários · celular"). O mesmo elemento é a
 * linha da tabela na web (quatro colunas) e o cartão do celular (nome, "papel, último uso" e o
 * chip), por grade e `md:contents`. É um link para `/usuarios/:id`; a selecionada leva `bg-line`.
 * O corte do cartão fica no miolo: `clip-path` no link cortaria o anel de foco.
 */
function UserRow({ user, isMe, now }: UserRowProps) {
  return (
    <li>
      <NavLink to={`/usuarios/${user.id}`} className="group block">
        <span className="grid min-h-11 grid-cols-[1fr_auto] items-center gap-x-3 gap-y-0.5 px-4 py-3.5 group-aria-[current=page]:bg-line max-md:cut-lg max-md:bg-panel md:min-h-16 md:grid-cols-[2.2fr_1.1fr_1.6fr_1fr] md:gap-4 md:border-b md:border-line md:px-5 md:py-0">
          <span className="flex min-w-0 flex-col gap-0.5">
            <span className="text-body font-semibold md:text-body-sm">
              {isMe ? `${user.name} (você)` : user.name}
            </span>
            <span className="truncate text-label font-normal text-text-muted max-md:hidden">
              {user.email}
            </span>
          </span>
          <span className="block text-label font-normal text-text-muted md:contents">
            <span className="md:order-2 md:text-body-sm md:text-text">
              {LABELS.role[user.role]}
            </span>
            <span className="md:hidden">, </span>
            <span className="md:order-4 md:text-body-sm">
              {user.lastUsedAt ? lastUse(user.lastUsedAt, now) : "nunca entrou"}
            </span>
          </span>
          <span className="max-md:col-start-2 max-md:row-span-2 max-md:row-start-1 md:order-3">
            <StatusChip tone={TONE[user.status]}>{LABELS.status[user.status]}</StatusChip>
          </span>
        </span>
      </NavLink>
    </li>
  )
}

export { UserRow }
