import { Loader2Icon } from "lucide-react"
import * as React from "react"
import { Link, Outlet, useMatch, useParams } from "react-router"

import type { ApiError } from "@/shared/api/api-error"
import type { Page } from "@/shared/api/page"
import { cn } from "@/shared/lib/cn"
import { MD_UP, useMediaQuery } from "@/shared/lib/use-media-query"
import { Alert } from "@/shared/ui/alert"
import { Button } from "@/shared/ui/button"

import { UserRow } from "../components/user-row"
import { useCurrentUser, useInfiniteUsers, useUsers } from "../iam.hooks"
import { errorMessage } from "../iam.messages"
import type { UserSummary } from "../iam.service"

// Os mockups mostram 5 linhas por página ("Mostrando 1 a 5 de 54 usuários").
const PAGE_SIZE = 5

type ListProps = { meId: string; now: Date }

type BodyProps = ListProps & {
  users: UserSummary[] | undefined
  error: unknown
  footer: React.ReactNode
}

/** Erro, carregando ou as linhas (com as colunas na web), e o rodapé de paginação. Lista vazia não mostra nada. */
function Body({ users, error, footer, meId, now }: BodyProps) {
  const failure = error as Partial<ApiError> | null

  return (
    <>
      {failure && (
        <div className="md:p-5">
          <Alert tone="danger">{errorMessage(failure)}</Alert>
        </div>
      )}

      {!users && !failure && (
        <div role="status" aria-busy="true" className="flex justify-center py-6">
          <Loader2Icon
            aria-hidden="true"
            className="size-6 animate-spin text-text-muted"
          />
        </div>
      )}

      {users && users.length > 0 && (
        <>
          <div
            aria-hidden="true"
            className="grid grid-cols-[2.2fr_1.1fr_1.6fr_1fr] gap-4 border-b border-line px-5 py-4 text-label text-text-muted max-md:hidden"
          >
            <span>Usuário</span>
            <span>Papel</span>
            <span>Situação</span>
            <span>Último uso</span>
          </div>
          <ul className="flex flex-col gap-3 md:gap-0">
            {users.map((user) => (
              <UserRow key={user.id} user={user} isMe={user.id === meId} now={now} />
            ))}
          </ul>
          {footer}
        </>
      )}
    </>
  )
}

/** Web: uma página por vez, com "Anterior" e "Próxima" (mockup "O próprio usuário e paginação, web"). */
function WebList(props: ListProps) {
  const [page, setPage] = React.useState(0)
  const [lastGood, setLastGood] = React.useState<Page<UserSummary>>()
  const query = useUsers({ page, size: PAGE_SIZE })

  // Página que falha não leva as linhas: a última boa fica na tela, com o aviso e a navegação.
  if (query.data && query.data !== lastGood) setLastGood(query.data)

  // Linhas, texto e botões seguem o que está na tela (a anterior enquanto a nova carrega), não o `page`.
  const shown = query.data ?? lastGood
  const from = shown ? shown.page * shown.size + 1 : 0

  return (
    <Body
      {...props}
      users={shown?.content}
      error={query.error}
      footer={
        shown && (
          <div className="flex items-center justify-between gap-4 px-5 py-3">
            <p className="text-label font-normal text-text-muted tabular-nums">
              {`Mostrando ${from} a ${from + shown.content.length - 1} de ${shown.totalElements} usuários`}
            </p>
            <div className="flex gap-2">
              <Button
                variant="outline"
                disabled={shown.page === 0 || query.isPlaceholderData}
                onClick={() => setPage(shown.page - 1)}
                className="text-body-sm [&>span]:bg-panel"
              >
                Anterior
              </Button>
              {/* Sem rótulo de "tentar de novo" aprovado: Próxima refaz o pedido da página que falhou. */}
              <Button
                variant="outline"
                disabled={shown.page + 1 >= shown.totalPages || query.isPlaceholderData}
                onClick={() =>
                  shown.page + 1 === page ? void query.refetch() : setPage(shown.page + 1)
                }
                className="text-body-sm [&>span]:bg-panel"
              >
                Próxima
              </Button>
            </div>
          </div>
        )
      }
    />
  )
}

/** Celular: as páginas se acumulam com "Carregar mais" (mockup "Lista com paginação, celular"). */
function MobileList(props: ListProps) {
  const query = useInfiniteUsers(PAGE_SIZE)
  const pages = query.data?.pages
  const users = pages?.flatMap((page) => page.content)

  return (
    <Body
      {...props}
      users={users}
      error={query.error}
      footer={
        users &&
        pages && (
          <div className="flex flex-col gap-3 pt-3">
            <p className="text-center text-label font-normal text-text-muted tabular-nums">
              {`Mostrando ${users.length} de ${pages[pages.length - 1].totalElements} usuários`}
            </p>
            <Button
              variant="outline"
              size="lg"
              disabled={!query.hasNextPage}
              loading={query.isFetchingNextPage}
              onClick={() => void query.fetchNextPage()}
              className="w-full"
            >
              Carregar mais
            </Button>
          </div>
        )
      }
    />
  )
}

/**
 * Lista de usuários (mockups "Usuários · web" e "Usuários · celular"), só do Administrador.
 * Rota de layout de `/usuarios`: com `/usuarios/:id` a web mostra a lista e o detalhe lado a
 * lado (o `Outlet` da região de detalhe é o encaixe), e o celular troca a lista pela tela de
 * detalhe, que tem cabeçalho próprio. No celular o título da barra superior é o da casca, e o
 * botão de convidar fica no rodapé fixo.
 */
function UsersScreen() {
  const { data: me } = useCurrentUser()
  const { id } = useParams()
  const isDesktop = useMediaQuery(MD_UP)
  // `/usuarios/convidar` é rota irmã de `:id` (sem `id`): no celular também troca a lista pela tela.
  const inviting = useMatch("/usuarios/convidar") !== null
  const selected = id !== undefined || inviting

  if (!me) return null

  const now = new Date()
  const List = isDesktop ? WebList : MobileList

  return (
    <main
      className={cn(
        // No celular o cabeçalho é só o título oculto e o rodapé fixo: sem vão entre ele e a lista.
        "flex flex-col gap-6 max-md:gap-0 md:px-10 md:py-8",
        !selected && "max-md:px-4 max-md:pt-4 max-md:pb-28",
      )}
    >
      <div
        className={cn(
          "flex flex-wrap items-center justify-between gap-4",
          selected && "max-md:hidden",
        )}
      >
        <h1 className="text-display-lg max-md:sr-only">Usuários</h1>
        <div className="max-md:fixed max-md:inset-x-0 max-md:bottom-0 max-md:border-t max-md:border-line max-md:bg-nav max-md:px-4 max-md:pt-3 max-md:pb-5">
          <Button
            size="lg"
            nativeButton={false}
            render={<Link to="/usuarios/convidar" />}
            role="link"
            className="w-full md:min-h-11 md:w-auto md:cut-sm md:px-5.5"
          >
            Convidar usuário
          </Button>
        </div>
      </div>

      <div className="flex flex-wrap items-start gap-6">
        <section
          aria-label="Lista de usuários"
          className={cn(
            "min-w-0 flex-[999_1_35rem] md:cut-lg md:overflow-x-auto md:bg-panel",
            selected && "max-md:hidden",
          )}
        >
          <div className="md:min-w-155">
            <List meId={me.id} now={now} />
          </div>
        </section>

        {selected && (
          // O convite é um diálogo na web (fora do fluxo): sem região reservada ao lado da lista.
          <div
            className={cn(
              "min-w-0 flex-[1_1_21.25rem] max-md:w-full md:max-w-100",
              inviting && "md:hidden",
            )}
          >
            <Outlet />
          </div>
        )}
      </div>
    </main>
  )
}

export { UsersScreen }
