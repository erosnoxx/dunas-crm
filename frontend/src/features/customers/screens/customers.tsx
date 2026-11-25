import {
  CircleAlertIcon,
  ContactIcon,
  InfoIcon,
  type LucideIcon,
  PlusIcon,
  RotateCwIcon,
  SearchIcon,
  WifiOffIcon,
} from "lucide-react"
import * as React from "react"
import { Link, useLocation, useSearchParams } from "react-router"

import type { ApiError } from "@/shared/api/api-error"
import { cn } from "@/shared/lib/cn"
import { MD_UP, useMediaQuery } from "@/shared/lib/use-media-query"
import { Button } from "@/shared/ui/button"
import { Input } from "@/shared/ui/input"
import { Label } from "@/shared/ui/label"
import { NativeSelect } from "@/shared/ui/native-select"
import { Spinner } from "@/shared/ui/spinner"

import {
  useCustomers,
  useCustomersInfinite,
  useDeactivatedCount,
} from "../customers.hooks"
import { errorMessage, sentence } from "../customers.messages"
import type { CustomerSummary, StatusFilter } from "../customers.service"
import { CustomerCard, CustomerTable } from "../components/customer-row"

const PAGE_SIZE = 20

// Ordem decidida na Tarefa 1 (telas 2 a 6 e o catálogo); o padrão é `ACTIVE`.
const STATUS_OPTIONS: [StatusFilter, string][] = [
  ["ACTIVE", "Ativos"],
  ["DEACTIVATED", "Desativados"],
  ["ALL", "Todos"],
]

// Busca e situação de 44 px, fundo `panel` e contorno `line-strong` nos dois tamanhos
// (Main.dc.html:30, Clientes-celular.dc.html:22 e 26).
const FILTER_FIELD = "h-11 md:bg-panel"

// Texto e ação dos estados (Clientes-web-vazio.dc.html:35-36): 14/20 `text-muted` até 520 px;
// link de 44 px, 12 de respiro, 14 600 `brand-text`.
const STATE_TEXT = "max-w-130 text-body-sm text-text-muted"
const STATE_ACTION =
  "inline-flex min-h-11 items-center justify-center px-3 text-body-sm font-semibold text-brand-text"

const NETWORK_TEXT =
  "Sem resposta do servidor. Confira a conexão e tente de novo; a busca e o filtro ficam como estão."

// A navegação da própria digitação: a URL chega depois da tecla (o router atualiza em transição),
// então ela não pode devolver o texto ao campo.
const TYPING = { typing: true }

type Filters = { q: string; status: StatusFilter }

/** O pedido da lista: texto vazio não vai; o texto vai como digitado (a API tira a pontuação: ADR-11). */
const listInput = ({ q, status }: Filters) => ({ ...(q && { text: q }), status })

/**
 * Moldura dos estados no lugar da lista: no celular a da tela 3 (32 px acima, respiro 24/16, ícone
 * de 32), na web a das telas 4 e 6 (respiro 48/24, ícone de 40); `panel` com contorno `line`, 10 px
 * entre os itens, título em `text-title`.
 */
function StateFrame({
  icon: Icon,
  iconClassName = "text-text-muted",
  title,
  role,
  children,
}: {
  icon: LucideIcon
  iconClassName?: string
  title: string
  role?: "alert"
  children?: React.ReactNode
}) {
  return (
    <section
      role={role}
      aria-live={role ? undefined : "polite"}
      className="flex flex-col items-center gap-2.5 border border-line bg-panel px-4 py-6 text-center max-md:mt-8 md:px-6 md:py-12"
    >
      <Icon aria-hidden="true" className={cn("size-8 md:size-10", iconClassName)} />
      <h2 className="text-title">{title}</h2>
      {children}
    </section>
  )
}

/** Erro ao listar (tela 6): o texto da falha de rede, ou a mensagem do `code`, e "Tentar de novo". */
function LoadError({ error, onRetry }: { error: unknown; onRetry: () => void }) {
  const failure = error as Partial<ApiError>

  return (
    <StateFrame
      role="alert"
      icon={WifiOffIcon}
      iconClassName="text-danger"
      title="Não foi possível carregar os clientes"
    >
      <p className="flex max-w-140 items-start gap-1.5 text-left text-body-sm text-danger-text">
        <CircleAlertIcon
          aria-hidden="true"
          strokeWidth={2.5}
          className="mt-px size-4.5 shrink-0 text-danger"
        />
        {failure.status === 0 ? NETWORK_TEXT : errorMessage(failure)}
      </p>
      <button
        type="button"
        onClick={onRetry}
        className="cut-sm inline-flex min-h-11 items-center gap-2 bg-chip-ok px-5 text-body-sm font-semibold text-text"
      >
        <RotateCwIcon aria-hidden="true" strokeWidth={2.5} className="size-4.5" />
        Tentar de novo
      </button>
    </StateFrame>
  )
}

const SUBJECT: Record<StatusFilter, string> = {
  ACTIVE: "Nenhum cliente ativo",
  DEACTIVATED: "Nenhum cliente desativado",
  ALL: "Nenhum cliente",
}

/** Sem resultado com texto ou filtro (tela 3). O "Novo cliente" é o da tela (topo ou rodapé). */
function NoResult({
  filters: { q, status },
  canEdit,
  onClear,
}: {
  filters: Filters
  canEdit: boolean
  onClear: () => void
}) {
  const what = q
    ? `${SUBJECT[status]} tem "${q}" no nome, no documento, numa cidade ou no número de série de um equipamento.`
    : `${SUBJECT[status]} cadastrado.`
  const hints = [
    q && "confira a busca",
    status === "ACTIVE" && "veja os desativados",
    canEdit && "cadastre o cliente",
  ].filter((hint): hint is string => Boolean(hint))
  const next =
    hints.length > 1 ? `${hints.slice(0, -1).join(", ")} ou ${hints.at(-1)}` : hints[0]

  return (
    <StateFrame icon={SearchIcon} title="Nenhum cliente encontrado">
      <p className={STATE_TEXT}>{next ? `${what} ${sentence(next)}` : what}</p>
      <button type="button" onClick={onClear} className={STATE_ACTION}>
        Limpar busca e filtros
      </button>
    </StateFrame>
  )
}

/**
 * Lista padrão vazia (sem texto, ativos). Só aqui pergunta pelos desativados (ADR-28): nenhum é a
 * tela 4; algum é o D2, com "Ver desativados".
 */
function DefaultEmpty({
  canEdit,
  onShowDeactivated,
}: {
  canEdit: boolean
  onShowDeactivated: () => void
}) {
  const deactivated = useDeactivatedCount(true)

  if (deactivated.isPending) return <Spinner />

  if (deactivated.data)
    return (
      <StateFrame icon={ContactIcon} title="Nenhum cliente ativo">
        <p className={STATE_TEXT}>
          Todos os clientes cadastrados estão desativados. Veja os desativados para
          reativar um deles.
        </p>
        <button type="button" onClick={onShowDeactivated} className={STATE_ACTION}>
          Ver desativados
        </button>
      </StateFrame>
    )

  return (
    <StateFrame icon={ContactIcon} title="Nenhum cliente cadastrado">
      {canEdit && (
        <>
          <p className={STATE_TEXT}>
            Cadastre o cliente com o CPF ou o CNPJ e um contato. Depois, na ficha dele,
            cadastre os locais e os equipamentos instalados em cada um.
          </p>
          <Link to="/clientes/novo" className={STATE_ACTION}>
            Cadastrar o primeiro cliente
          </Link>
        </>
      )}
    </StateFrame>
  )
}

type BodyProps = {
  customers: CustomerSummary[] | undefined
  error: unknown
  onRetry: () => void
  empty: React.ReactNode
  children: (customers: CustomerSummary[]) => React.ReactNode
}

/** Erro, carregando, vazio ou os clientes. */
function Body({ customers, error, onRetry, empty, children }: BodyProps) {
  if (error) return <LoadError error={error} onRetry={onRetry} />
  if (!customers) return <Spinner />
  if (customers.length === 0) return empty

  return children(customers)
}

type ListProps = { filters: Filters; empty: React.ReactNode }

/** Web: a tabela da tela 1, uma página por vez, com "Anterior" e "Próxima", como o catálogo. */
function WebList({ filters, empty }: ListProps) {
  const [page, setPage] = React.useState(0)
  const key = `${filters.q}\n${filters.status}`
  const [seen, setSeen] = React.useState(key)

  // Filtro novo recomeça da primeira página.
  if (seen !== key) {
    setSeen(key)
    setPage(0)
  }

  const query = useCustomers({ ...listInput(filters), page, size: PAGE_SIZE })
  const shown = query.data

  return (
    <Body
      customers={shown?.content}
      error={query.error}
      onRetry={() => void query.refetch()}
      empty={empty}
    >
      {(customers) =>
        shown && (
          <section
            aria-label="Lista de clientes"
            className="min-w-0 border border-line bg-panel"
          >
            <div className="overflow-x-auto">
              <CustomerTable customers={customers} text={filters.q} />
            </div>
            <div className="flex items-center justify-between gap-4 border-t border-line px-4 py-3">
              <p className="text-label font-normal text-text-muted tabular-nums">
                {`Mostrando ${shown.page * shown.size + 1} a ${shown.page * shown.size + customers.length} de ${shown.totalElements} clientes`}
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
                <Button
                  variant="outline"
                  disabled={shown.page + 1 >= shown.totalPages || query.isPlaceholderData}
                  onClick={() => setPage(shown.page + 1)}
                  className="text-body-sm [&>span]:bg-panel"
                >
                  Próxima
                </Button>
              </div>
            </div>
          </section>
        )
      }
    </Body>
  )
}

/** Celular: cartões (telas 2 e 5) que se acumulam com "Carregar mais", como o catálogo. */
function MobileList({ filters, empty }: ListProps) {
  const query = useCustomersInfinite({ ...listInput(filters), size: PAGE_SIZE })
  const pages = query.data?.pages
  const total = pages?.at(-1)?.totalElements ?? 0

  return (
    <Body
      customers={pages?.flatMap((page) => page.content)}
      error={query.error}
      onRetry={() => void query.refetch()}
      empty={empty}
    >
      {(customers) => (
        <>
          {/* Clientes-celular-tecnico.dc.html:37: 13/18 `text-muted`, peso 400. */}
          {filters.q && (
            <p aria-live="polite" className="text-label font-normal text-text-muted">
              {total === 1 ? "1 cliente encontrado" : `${total} clientes encontrados`}
            </p>
          )}
          <ul className="flex flex-col gap-3">
            {customers.map((customer) => (
              <CustomerCard key={customer.id} customer={customer} text={filters.q} />
            ))}
          </ul>
          <div className="flex flex-col gap-3">
            <p className="text-center text-label font-normal text-text-muted tabular-nums">
              {`Mostrando ${customers.length} de ${total} clientes`}
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
        </>
      )}
    </Body>
  )
}

/** Busca e situação vivem na URL (`?q=` e `?situacao=`), para "Ver cliente com este documento" (ADR-23). */
function useFilters() {
  const [params, setParams] = useSearchParams()
  const location = useLocation()
  const raw = params.get("situacao")
  const filters: Filters = {
    q: params.get("q") ?? "",
    status: STATUS_OPTIONS.find(([value]) => value === raw)?.[0] ?? "ACTIVE",
  }

  // O campo tem estado próprio; a navegação de fora (menu, "Limpar", voltar) o reescreve.
  const [text, setText] = React.useState(filters.q)
  const [seenKey, setSeenKey] = React.useState(location.key)
  if (seenKey !== location.key) {
    setSeenKey(location.key)
    if (!(location.state as typeof TYPING | null)?.typing) setText(filters.q)
  }

  const apply = (next: Filters, state?: typeof TYPING) => {
    const search = new URLSearchParams()
    if (next.q) search.set("q", next.q)
    if (next.status !== "ACTIVE") search.set("situacao", next.status)
    setParams(search, { replace: true, state })
  }

  const type = (value: string) => {
    setText(value)
    apply({ ...filters, q: value }, TYPING)
  }

  return { filters, text, type, apply }
}

/**
 * Clientes (telas 1 a 6, D1 e D2). Todos os papéis consultam; "Novo cliente" só para quem edita
 * (ADR-7), na barra dos filtros na web e fixo acima da barra inferior no celular. O Técnico vê o
 * aviso de que só consulta. No celular o título é o da barra da casca.
 */
function CustomersScreen({ canEdit }: { canEdit: boolean }) {
  const isDesktop = useMediaQuery(MD_UP)
  const id = React.useId()
  const { filters, text, type, apply } = useFilters()
  const List = isDesktop ? WebList : MobileList

  // Dica e placeholder das telas 1 (web) e 2 (celular): 12/16 `text-muted`, peso 400; no celular,
  // 6 px sob o campo (o espaço da coluna); na web, 8 px sob a barra (-12 dos 20 entre os blocos).
  const hint = (
    <p id={`${id}-hint`} className="text-caption font-normal text-text-muted md:-mt-3">
      {isDesktop
        ? "A busca também acha o cliente pelo número de série de um equipamento instalado nele: digite a série da etiqueta."
        : "Também acha pelo número de série do equipamento."}
    </p>
  )
  const label = "text-text-muted max-md:sr-only"

  return (
    <main
      className={cn(
        "flex flex-col gap-3 p-4 md:gap-5 md:p-8",
        // O botão fixo (72 px) fica sobre o fim da lista: a folga dele soma aos 16 px da página.
        canEdit && "max-md:pb-22",
      )}
    >
      <h1 className="text-display-lg max-md:sr-only">Clientes</h1>

      {!canEdit && (
        <p className="flex items-start gap-2 bg-chip-ok px-3 py-2.5 text-body-sm">
          <InfoIcon
            aria-hidden="true"
            strokeWidth={2.5}
            className="mt-0.5 size-4.5 shrink-0"
          />
          Seu papel (Técnico) consulta os clientes. Quem cadastra e altera é o
          Administrador e o Comercial.
        </p>
      )}

      {/* Quebra linha quando a busca ficaria abaixo de 240 px (768 a 1023, sem mockup), como o catálogo. */}
      <div className="flex flex-col gap-3 md:flex-row md:flex-wrap md:items-end">
        <div className="flex flex-col gap-1.5 md:min-w-60 md:flex-1">
          <Label htmlFor={`${id}-q`} className={label}>
            Buscar cliente
          </Label>
          <Input
            id={`${id}-q`}
            type="search"
            size="compact"
            value={text}
            onChange={(event) => type(event.target.value)}
            placeholder={
              isDesktop
                ? "Nome, nome fantasia, CPF/CNPJ, cidade ou número de série"
                : "Nome, documento, cidade ou nº de série"
            }
            aria-describedby={`${id}-hint`}
            className={cn(FILTER_FIELD, "md:text-body-sm")}
          />
          {!isDesktop && hint}
        </div>
        <div className="flex flex-col gap-1.5 md:w-45">
          <Label htmlFor={`${id}-status`} className={label}>
            Situação
          </Label>
          <NativeSelect
            id={`${id}-status`}
            size="compact"
            value={filters.status}
            onChange={(event) =>
              apply({ ...filters, status: event.target.value as StatusFilter })
            }
            className={cn(FILTER_FIELD, "pl-2.5 text-body-sm md:pl-3")}
          >
            {STATUS_OPTIONS.map(([value, text]) => (
              <option key={value} value={value}>
                {text}
              </option>
            ))}
          </NativeSelect>
        </div>
        {canEdit && (
          <div className="max-md:fixed max-md:inset-x-0 max-md:bottom-16 max-md:z-10 max-md:bg-ground max-md:px-4 max-md:py-3">
            <Button
              size="lg"
              nativeButton={false}
              render={<Link to="/clientes/novo" />}
              role="link"
              className="w-full max-md:min-h-12 md:min-h-11 md:w-auto md:cut-sm md:px-5 md:text-body-sm md:font-semibold"
            >
              <PlusIcon aria-hidden="true" strokeWidth={2.5} className="size-4.5" />
              Novo cliente
            </Button>
          </div>
        )}
      </div>
      {isDesktop && hint}

      <List
        filters={filters}
        empty={
          filters.q === "" && filters.status === "ACTIVE" ? (
            <DefaultEmpty
              canEdit={canEdit}
              onShowDeactivated={() => apply({ q: "", status: "DEACTIVATED" })}
            />
          ) : (
            <NoResult
              filters={filters}
              canEdit={canEdit}
              onClear={() => apply({ q: "", status: "ACTIVE" })}
            />
          )
        }
      />
    </main>
  )
}

export { CustomersScreen }
