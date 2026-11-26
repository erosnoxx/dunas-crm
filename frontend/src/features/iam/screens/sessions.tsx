import { Loader2Icon } from "lucide-react"

import type { ApiError } from "@/shared/api/api-error"
import { Alert } from "@/shared/ui/alert"
import { Button } from "@/shared/ui/button"
import { MobileHeader } from "@/shared/ui/mobile-header"

import { SessionItem } from "../components/session-item"
import {
  useCurrentUser,
  useLogout,
  useRevokeOtherSessions,
  useRevokeSession,
  useSessions,
} from "../iam.hooks"
import { errorMessage } from "../iam.messages"

/**
 * Minhas sessões (mockups "Minhas sessões, celular" e "Minhas sessões, web"). Encerrar uma
 * sessão não pede confirmação (ADR-11); a lista é a do servidor, e o refetch dos hooks tira a
 * que acabou de sair. No celular a página tem o cabeçalho próprio do mockup (com "Voltar"), e a
 * casca não põe a barra superior nesta rota.
 */
function SessionsScreen() {
  const { data: me } = useCurrentUser()

  return me ? <Sessions userId={me.id} /> : null
}

function Sessions({ userId }: { userId: string }) {
  const sessions = useSessions(userId)
  const revokeOne = useRevokeSession()
  const revokeOthers = useRevokeOtherSessions()
  const logout = useLogout()

  const list = sessions.data
  const hasOthers = list?.some((session) => !session.current) ?? false
  const error = (sessions.error ??
    revokeOne.error ??
    revokeOthers.error ??
    logout.error) as Partial<ApiError> | null
  const now = new Date()

  return (
    <div className="flex flex-col">
      {/* Celular: cabeçalho próprio com Voltar (a casca não põe barra superior aqui). Web: só o h1. */}
      <MobileHeader
        to="/"
        title="Minhas sessões"
        className="md:h-auto md:border-0 md:bg-transparent md:px-10 md:pt-8"
        backClassName="md:hidden"
        titleClassName="md:text-display-lg"
      />
      <main className="flex flex-col gap-4 px-4 pt-5 pb-6 md:gap-6 md:px-10 md:pt-6 md:pb-8">
        <p className="text-body-sm text-text-muted md:max-w-140">
          Aparelhos conectados à sua conta. Encerre os que você não reconhece.
        </p>
        {/* No celular é o rodapé (sem fixar no fim da tela); na web continua o parágrafo acima (a margem negativa cancela o gap). */}
        <p className="text-caption text-text-muted max-md:order-last md:-mt-6 md:text-body-sm md:max-w-140">
          A sessão dura 30 dias e renova a cada uso.
        </p>

        {error && (
          <Alert tone="danger" className="md:max-w-160">
            {errorMessage(error)}
          </Alert>
        )}

        {sessions.isPending && (
          <div role="status" aria-busy="true" className="flex justify-center py-6">
            <Loader2Icon
              aria-hidden="true"
              className="size-6 animate-spin text-text-muted"
            />
          </div>
        )}

        {list && list.length > 0 && (
          <ul className="flex flex-col gap-3 md:max-w-160">
            {list.map((session) => (
              <SessionItem
                key={session.id}
                session={session}
                now={now}
                pending={revokeOne.isPending && revokeOne.variables === session.id}
                onRevoke={revokeOne.mutate}
              />
            ))}
          </ul>
        )}

        <div className="mt-1 flex flex-col gap-3 md:mt-0">
          <Button
            variant="outline"
            size="lg"
            disabled={!hasOthers}
            loading={revokeOthers.isPending}
            onClick={() => revokeOthers.mutate(userId)}
            className="text-danger-text md:min-h-12 md:cut-sm md:[&>span]:cut-sm md:self-start"
          >
            Encerrar todas as outras
          </Button>
          <Button
            size="lg"
            disabled={logout.isPending}
            onClick={() => logout.mutate()}
            className="md:hidden"
          >
            Sair deste aparelho
          </Button>
        </div>
      </main>
    </div>
  )
}

export { SessionsScreen }
