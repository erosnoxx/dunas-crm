import * as React from "react"
import { toast } from "sonner"

import type { ApiError } from "@/shared/api/api-error"
import { cn } from "@/shared/lib/cn"
import { useMediaQuery } from "@/shared/lib/use-media-query"
import { Alert } from "@/shared/ui/alert"
import { Button } from "@/shared/ui/button"
import { Panel } from "@/shared/ui/panel"
import { StatusChip } from "@/shared/ui/status-chip"

import {
  useChangeUserRole,
  useDeactivateUser,
  useReactivateUser,
  useResendInvitation,
  useRevokeAllSessions,
  useRevokeOtherSessions,
  useRevokeSession,
} from "../iam.hooks"
import { displayName, errorMessage, LABELS, ROLES, STATUS_TONE } from "../iam.messages"
import type { Role, SessionSummary, UserSummary } from "../iam.service"
import { SessionItem } from "./session-item"

// Textos aprovados em docs/system-design/01-iam/design/telas-complementares.md.
const SELF_NOTE = "Você não pode mudar o seu papel nem desativar a sua conta."
const DEACTIVATED_TEXT =
  "As sessões foram encerradas e este usuário não consegue entrar. Ao reativar, ele volta a entrar com a senha que já tem."
const invitedText = (name: string) =>
  `${name.split(" ")[0]} ainda não definiu a senha. Reenviar o convite invalida o link anterior.`

type Callbacks = { onError: (error: unknown) => void; onSuccess?: () => void }

/**
 * As ações do painel. Cada uma limpa o aviso da anterior, não pede confirmação (ADR-11) e ao
 * terminar os hooks invalidam a lista e o detalhe; o erro de qualquer uma sai num aviso só.
 */
function useActions(userId: string) {
  const [error, setError] = React.useState<Partial<ApiError> | null>(null)
  const revokeOne = useRevokeSession()
  const revokeAll = useRevokeAllSessions()
  const revokeOthers = useRevokeOtherSessions()
  const changeRole = useChangeUserRole()
  const deactivate = useDeactivateUser()
  const reactivate = useReactivateUser()
  const resend = useResendInvitation()

  const run = (
    pending: boolean,
    start: (callbacks: Callbacks) => void,
    done?: () => void,
  ) => {
    if (pending) return

    setError(null)
    start({ onError: (e) => setError(e as Partial<ApiError>), onSuccess: done })
  }

  return {
    error,
    revokingId: revokeOne.isPending ? revokeOne.variables : undefined,
    revokingAll: revokeAll.isPending,
    revokingOthers: revokeOthers.isPending,
    changingRole: changeRole.isPending,
    deactivating: deactivate.isPending,
    reactivating: reactivate.isPending,
    resending: resend.isPending,
    revoke: (id: string) =>
      run(revokeOne.isPending && revokeOne.variables === id, (c) =>
        revokeOne.mutate(id, c),
      ),
    revokeAll: () => run(revokeAll.isPending, (c) => revokeAll.mutate(userId, c)),
    revokeOthers: () =>
      run(revokeOthers.isPending, (c) => revokeOthers.mutate(userId, c)),
    changeRole: (role: Role) =>
      run(changeRole.isPending, (c) => changeRole.mutate({ id: userId, role }, c)),
    deactivate: () => run(deactivate.isPending, (c) => deactivate.mutate(userId, c)),
    reactivate: () => run(reactivate.isPending, (c) => reactivate.mutate(userId, c)),
    // "Convite reenviado" é o aviso de sucesso aprovado (telas-complementares.md, sem artboard).
    resend: () =>
      run(
        resend.isPending,
        (c) => resend.mutate(userId, c),
        () => toast.success("Convite reenviado"),
      ),
  }
}

type Actions = ReturnType<typeof useActions>

/** Papel como lista nativa (mockup do painel): troca na hora, sem confirmação, e volta ao do servidor se falhar. */
function RoleSelect({ role, actions }: { role: Role; actions: Actions }) {
  const id = React.useId()

  return (
    <div className="flex flex-col gap-1.5">
      <label htmlFor={id} className="text-label">
        Papel
      </label>
      <select
        id={id}
        value={role}
        disabled={actions.changingRole}
        onChange={(event) => actions.changeRole(event.target.value as Role)}
        className="h-13 border border-line-strong bg-ground px-3 text-body text-text disabled:opacity-50 md:h-12"
      >
        {ROLES.map((value) => (
          <option key={value} value={value}>
            {LABELS.role[value]}
          </option>
        ))}
      </select>
    </div>
  )
}

type SessionsProps = {
  sessions: SessionSummary[]
  self: boolean
  isDesktop: boolean
  actions: Actions
  now: Date
}

/** Sessões ativas, com "Encerrar" em cada uma e o botão de encerrar todas (as outras, no próprio usuário). */
function Sessions({ sessions, self, isDesktop, actions, now }: SessionsProps) {
  const Heading = isDesktop ? "h3" : "h2"
  const everyone = self ? actions.revokeOthers : actions.revokeAll
  const hasOthers = sessions.some((session) => !session.current)

  return (
    <div className="flex flex-col gap-3">
      <div className="flex items-baseline justify-between">
        <Heading className={isDesktop ? "text-label" : "text-body font-semibold"}>
          Sessões ativas
        </Heading>
        <span className="text-label font-normal text-text-muted tabular-nums">
          {sessions.length}
        </span>
      </div>
      {sessions.length > 0 && (
        <ul className="flex flex-col gap-3">
          {sessions.map((session) => (
            <SessionItem
              key={session.id}
              session={session}
              now={now}
              layout={self ? "stacked" : "inline"}
              pending={actions.revokingId === session.id}
              onRevoke={actions.revoke}
            />
          ))}
        </ul>
      )}
      <Button
        variant="outline"
        size={isDesktop ? "md" : "lg"}
        disabled={self ? !hasOthers : sessions.length === 0}
        loading={self ? actions.revokingOthers : actions.revokingAll}
        onClick={everyone}
        className={cn(
          "w-full text-sm text-danger-text md:[&>span]:px-4 [&>span]:bg-panel",
          !self && "max-md:text-base",
        )}
      >
        {self ? "Encerrar todas as outras" : "Encerrar todas as sessões"}
      </Button>
    </div>
  )
}

const DEACTIVATE_LINK = "min-h-11 self-start text-sm underline"

type UserPanelProps = {
  user: UserSummary
  sessions: SessionSummary[]
  isSelf: boolean
}

/**
 * Detalhe do usuário (mockups "Usuários · web", "Usuário e sessões", "Usuário desativado",
 * "Convite pendente" e "O próprio usuário", web e celular). Cada situação mostra só as ações
 * dela: ativo (sessões, papel, desativar), desativado (reativar), convite pendente (reenviar e
 * desativar; o papel só se muda com a conta ativa) e o próprio (sem trocar o papel nem desativar:
 * ADR-5). Se o servidor ainda recusar com `cannot_*`, a mensagem sai no aviso. O nome é o h2 do
 * painel na web; no celular é o h1 do cabeçalho da tela (`UserDetailScreen`).
 */
function UserPanel({ user, sessions, isSelf }: UserPanelProps) {
  const isDesktop = useMediaQuery("(min-width: 48rem)")
  const headingId = React.useId()
  const actions = useActions(user.id)
  const now = new Date()

  const active = user.status === "ACTIVE"
  const invited = user.status === "INVITED"
  const deactivated = user.status === "DEACTIVATED"
  const manageable = !isSelf && (active || invited)
  const failure = actions.error
  const chip = (
    <StatusChip tone={STATUS_TONE[user.status]}>{LABELS.status[user.status]}</StatusChip>
  )
  const title = displayName(user.name, isSelf)
  // Só quem está ativo tem sessões; o servidor já manda a lista vazia nos outros casos.
  const showSessions = active

  const alert = failure && <Alert tone="danger">{errorMessage(failure)}</Alert>

  const primary = invited ? (
    <Button
      size={isDesktop ? "md" : "lg"}
      loading={actions.resending}
      onClick={actions.resend}
      className="w-full md:min-h-12 md:px-6"
    >
      Reenviar convite
    </Button>
  ) : deactivated ? (
    <Button
      size={isDesktop ? "md" : "lg"}
      loading={actions.reactivating}
      onClick={actions.reactivate}
      className="w-full md:min-h-12 md:px-6"
    >
      Reativar usuário
    </Button>
  ) : null

  const deactivateLink = manageable && (
    <Button
      variant="danger-text"
      loading={actions.deactivating}
      onClick={actions.deactivate}
      className={DEACTIVATE_LINK}
    >
      Desativar usuário
    </Button>
  )

  if (isDesktop)
    return (
      <Panel aria-labelledby={headingId} className="flex flex-col gap-5 p-6">
        <div className={cn("flex flex-col", manageable && active ? "gap-1" : "gap-2")}>
          <h2 id={headingId} className="text-title">
            {title}
          </h2>
          <span className="text-body-sm text-text-muted">
            {manageable && active
              ? user.email
              : `${user.email}, ${LABELS.role[user.role]}`}
          </span>
          {!(manageable && active) && <span className="self-start">{chip}</span>}
        </div>

        {alert}

        {isSelf && <p className="text-body-sm text-text-muted">{SELF_NOTE}</p>}
        {invited && (
          <p className="text-body-sm text-text-muted">{invitedText(user.name)}</p>
        )}
        {deactivated && (
          <p className="text-body-sm text-text-muted">{DEACTIVATED_TEXT}</p>
        )}

        {showSessions && (
          <Sessions
            sessions={sessions}
            self={isSelf}
            isDesktop
            actions={actions}
            now={now}
          />
        )}
        {manageable && active && <RoleSelect role={user.role} actions={actions} />}
        {primary}
        {deactivateLink}
      </Panel>
    )

  const card = "cut-lg flex flex-col gap-3 bg-panel p-4"

  return (
    <>
      {alert}

      <section className={card}>
        <div className="flex items-center justify-between gap-3">
          <span className="min-w-0 text-body-sm break-words text-text-muted">
            {user.email}
          </span>
          {chip}
        </div>
        {manageable && active ? (
          <RoleSelect role={user.role} actions={actions} />
        ) : (
          <div className="flex flex-col gap-0.5">
            <span className="text-label">Papel</span>
            <span className="text-body">{LABELS.role[user.role]}</span>
            {isSelf && (
              <span className="mt-1 text-label font-normal text-text-muted">
                {SELF_NOTE}
              </span>
            )}
          </div>
        )}
      </section>

      {showSessions && (
        <section className={card}>
          <Sessions
            sessions={sessions}
            self={isSelf}
            isDesktop={false}
            actions={actions}
            now={now}
          />
        </section>
      )}
      {(invited || deactivated) && (
        <section className={cn(card, "gap-2")}>
          <h2 className="text-body font-semibold">
            {invited ? "Convite não aceito" : "Sem acesso"}
          </h2>
          <p className="text-body-sm text-text-muted">
            {invited ? invitedText(user.name) : DEACTIVATED_TEXT}
          </p>
        </section>
      )}

      {deactivateLink}

      {primary && (
        <div className="fixed inset-x-0 bottom-0 border-t border-line bg-nav px-4 pt-3 pb-5">
          {primary}
        </div>
      )}
    </>
  )
}

export { UserPanel }
