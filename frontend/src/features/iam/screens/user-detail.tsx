import { ChevronLeftIcon, Loader2Icon } from "lucide-react"
import type { ReactNode } from "react"
import { Link, useParams } from "react-router"

import { UNKNOWN_ERROR_MESSAGE, type ApiError } from "@/shared/api/api-error"
import { useMediaQuery } from "@/shared/lib/use-media-query"
import { Alert } from "@/shared/ui/alert"

import { UserPanel } from "../components/user-panel"
import { useCurrentUser, useUser } from "../iam.hooks"
import { displayName, messageFor, sentence } from "../iam.messages"

const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i

/**
 * No celular a tela tem o cabeçalho próprio do mockup (com "Voltar" e o nome no h1) e a casca
 * não põe a barra superior em `/usuarios/*`. Na web o nome é o h2 do painel; o h1 é o da lista.
 */
function Frame({ title, children }: { title?: string; children: ReactNode }) {
  const isDesktop = useMediaQuery("(min-width: 48rem)")

  return (
    <>
      {!isDesktop && (
        <header className="flex h-16 items-center gap-1 border-b border-line bg-nav px-2">
          <Link
            to="/usuarios"
            aria-label="Voltar"
            className="flex size-12 items-center justify-center"
          >
            <ChevronLeftIcon aria-hidden="true" className="size-6" />
          </Link>
          {title && <h1 className="text-title">{title}</h1>}
        </header>
      )}
      <div className="flex flex-col gap-4 max-md:p-4 max-md:pb-28">{children}</div>
    </>
  )
}

function Failure({ error }: { error: Partial<ApiError> }) {
  return (
    <Alert tone="danger">
      {sentence(messageFor(error.code, error.message || UNKNOWN_ERROR_MESSAGE))}
    </Alert>
  )
}

function Detail({ id }: { id: string }) {
  const { data: me } = useCurrentUser()
  const query = useUser(id)
  const failure = query.error as Partial<ApiError> | null
  const detail = query.data
  // Quem é o próprio usuário só se sabe com `me`: sem ele nenhuma ação aparece (nunca o seletor de papel dele).
  const isSelf = detail && me ? detail.user.id === me.id : false
  const ready = detail && me

  return (
    <Frame title={ready ? displayName(detail.user.name, isSelf) : undefined}>
      {failure && <Failure error={failure} />}
      {!ready && !failure && (
        <div role="status" aria-busy="true" className="flex justify-center py-6">
          <Loader2Icon
            aria-hidden="true"
            className="size-6 animate-spin text-text-muted"
          />
        </div>
      )}
      {ready && (
        <UserPanel user={detail.user} sessions={detail.sessions} isSelf={isSelf} />
      )}
    </Frame>
  )
}

/**
 * Detalhe de `/usuarios/:id`. Um `id` que não é UUID nem chega ao servidor: mostra o
 * `user_not_found` da tabela de erros (não há texto aprovado só para isto).
 */
function UserDetailScreen() {
  const { id = "" } = useParams()

  if (!UUID.test(id))
    return (
      <Frame>
        <Failure error={{ code: "user_not_found" }} />
      </Frame>
    )

  return <Detail id={id} />
}

export { UserDetailScreen }
