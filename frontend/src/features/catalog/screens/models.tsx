import { InfoIcon, PlusIcon } from "lucide-react"
import * as React from "react"
import { Link } from "react-router"

import type { ApiError } from "@/shared/api/api-error"
import type { Page } from "@/shared/api/page"
import { cn } from "@/shared/lib/cn"
import { MD_UP, useMediaQuery } from "@/shared/lib/use-media-query"
import { Alert } from "@/shared/ui/alert"
import { Button } from "@/shared/ui/button"
import { Input } from "@/shared/ui/input"
import { Label } from "@/shared/ui/label"
import { NativeSelect } from "@/shared/ui/native-select"
import { Spinner } from "@/shared/ui/spinner"

import {
  useEquipmentModels,
  useEquipmentTypes,
  useInfiniteEquipmentModels,
} from "../catalog.hooks"
import { errorMessage } from "../catalog.messages"
import type { EquipmentModelSummary, StatusFilter } from "../catalog.service"
import { CatalogTabs } from "../components/catalog-tabs"
import { ModelRow, ModelTableHeader } from "../components/model-row"

const PAGE_SIZE = 20
// shortcut: o filtro lista só os 100 primeiros tipos; paginar a escolha se o catálogo passar disso.
const TYPE_OPTIONS = { status: "ALL", page: 0, size: 100 } as const

type Filters = { text: string; typeId: string; status: StatusFilter }

const DEFAULT_FILTERS: Filters = { text: "", typeId: "", status: "ACTIVE" }

const STATUS_OPTIONS: [StatusFilter, string][] = [
  ["ACTIVE", "Ativos"],
  ["DEACTIVATED", "Desativados"],
  ["ALL", "Todos"],
]

const SCOPE: Record<StatusFilter, string> = {
  ACTIVE: " entre os modelos ativos",
  DEACTIVATED: " entre os modelos desativados",
  ALL: "",
}

// Os campos do filtro têm 44 px e o fundo `panel` nos dois tamanhos (mockups 1 e 2).
const FILTER_FIELD = "h-11 md:bg-panel"

/** O pedido da lista: texto e tipo vazios não vão. */
const listInput = ({ text, typeId, status }: Filters) => ({
  ...(text && { text }),
  ...(typeId && { typeId }),
  status,
})

/** Vazio e sem resultado (mockup 13). "Limpar busca e filtros" só aparece com filtro aplicado. */
function Empty({
  filters,
  canEdit,
  onClear,
}: {
  filters: Filters
  canEdit: boolean
  onClear: () => void
}) {
  const filtered =
    filters.text !== "" || filters.typeId !== "" || filters.status !== "ACTIVE"
  const what = filters.text
    ? `Nada no catálogo bate com "${filters.text}"`
    : "Nada no catálogo bate com os filtros"
  const next = canEdit ? "Confira a busca ou cadastre o modelo." : "Confira a busca."
  // Sem filtro, o texto é derivado (o mockup 13 só desenha a busca sem resultado).
  const text = filtered
    ? `${what}${SCOPE[filters.status]}. ${next}`
    : `O catálogo ainda não tem modelos ativos.${canEdit ? " Cadastre o primeiro." : ""}`

  return (
    <section
      aria-live="polite"
      className="mt-8 flex flex-col items-center gap-2.5 border border-line bg-panel px-4 py-6 text-center md:mt-0 md:border-0"
    >
      <h2 className="text-title">Nenhum modelo encontrado</h2>
      <p className="text-body-sm text-text-muted">{text}</p>
      {filtered && (
        <button
          type="button"
          onClick={onClear}
          className="inline-flex min-h-11 items-center justify-center px-3 text-body-sm font-semibold text-brand-text"
        >
          Limpar busca e filtros
        </button>
      )}
    </section>
  )
}

type BodyProps = {
  models: EquipmentModelSummary[] | undefined
  error: unknown
  footer: React.ReactNode
  empty: React.ReactNode
}

/** Erro, carregando, vazio ou as linhas (com o cabeçalho na web), e o rodapé de paginação. */
function Body({ models, error, footer, empty }: BodyProps) {
  const failure = error as Partial<ApiError> | null

  return (
    <>
      {failure && (
        <div className="md:p-5">
          <Alert tone="danger">{errorMessage(failure)}</Alert>
        </div>
      )}

      {!models && !failure && <Spinner />}

      {models && models.length === 0 && !failure && empty}

      {models && models.length > 0 && (
        <>
          <ModelTableHeader />
          <ul className="flex flex-col gap-3 md:gap-0">
            {models.map((model) => (
              <ModelRow key={model.id} model={model} />
            ))}
          </ul>
          {footer}
        </>
      )}
    </>
  )
}

type ListProps = { filters: Filters; empty: React.ReactNode }

/** Web: uma página por vez, com "Anterior" e "Próxima", como a lista de usuários. */
function WebList({ filters, empty }: ListProps) {
  const [page, setPage] = React.useState(0)
  const [lastGood, setLastGood] = React.useState<Page<EquipmentModelSummary>>()
  const [seen, setSeen] = React.useState(filters)

  // Filtro novo recomeça da primeira página (e a lista anterior fica na tela até a nova chegar).
  if (seen !== filters) {
    setSeen(filters)
    setPage(0)
  }

  const query = useEquipmentModels({ ...listInput(filters), page, size: PAGE_SIZE })

  // Página que falha não leva as linhas: a última boa fica na tela, com o aviso e a navegação.
  if (query.data && query.data !== lastGood) setLastGood(query.data)

  const shown = query.data ?? lastGood
  const from = shown ? shown.page * shown.size + 1 : 0

  return (
    <Body
      models={shown?.content}
      error={query.error}
      empty={empty}
      footer={
        shown && (
          <div className="flex items-center justify-between gap-4 px-4 py-3">
            <p className="text-label font-normal text-text-muted tabular-nums">
              {`Mostrando ${from} a ${from + shown.content.length - 1} de ${shown.totalElements} modelos`}
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

/** Celular: as páginas se acumulam com "Carregar mais", como a lista de usuários. */
function MobileList({ filters, empty }: ListProps) {
  const query = useInfiniteEquipmentModels({ ...listInput(filters), size: PAGE_SIZE })
  const pages = query.data?.pages
  const models = pages?.flatMap((page) => page.content)

  return (
    <Body
      models={models}
      error={query.error}
      empty={empty}
      footer={
        models &&
        pages && (
          <div className="flex flex-col gap-3 pt-3">
            <p className="text-center text-label font-normal text-text-muted tabular-nums">
              {`Mostrando ${models.length} de ${pages[pages.length - 1].totalElements} modelos`}
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

/** Os filtros: busca, tipo e situação (mockups 1 e 2: rótulos visíveis só na web). */
function FilterBar({
  filters,
  onChange,
  isDesktop,
  children,
}: {
  filters: Filters
  onChange: (filters: Filters) => void
  isDesktop: boolean
  children: React.ReactNode
}) {
  const id = React.useId()
  const types = useEquipmentTypes(TYPE_OPTIONS)
  const set = (patch: Partial<Filters>) => onChange({ ...filters, ...patch })
  const label = "text-text-muted max-md:sr-only"

  return (
    // Quebra linha quando a busca ficaria abaixo de 240 px (768 a 1023, sem mockup); em 1280 cabe.
    <div className="flex flex-col gap-3 md:flex-row md:flex-wrap md:items-end">
      <div className="flex flex-col gap-1.5 md:min-w-60 md:flex-1">
        <Label htmlFor={`${id}-text`} className={label}>
          Buscar modelo
        </Label>
        <Input
          id={`${id}-text`}
          type="search"
          size="compact"
          value={filters.text}
          onChange={(event) => set({ text: event.target.value })}
          placeholder={
            isDesktop ? "Nome do modelo ou fabricante" : "Buscar modelo ou fabricante"
          }
          className={cn(FILTER_FIELD, "md:text-body-sm")}
        />
      </div>
      <div className="flex gap-2 md:contents">
        <div className="flex min-w-0 flex-1 flex-col gap-1.5 md:w-50 md:flex-none">
          <Label htmlFor={`${id}-type`} className={label}>
            Tipo
          </Label>
          <NativeSelect
            id={`${id}-type`}
            size="compact"
            value={filters.typeId}
            onChange={(event) => set({ typeId: event.target.value })}
            className={cn(FILTER_FIELD, "pl-2.5 text-body-sm md:pl-3")}
          >
            <option value="">{isDesktop ? "Todos" : "Tipo: todos"}</option>
            {types.data?.content.map((type) => (
              <option key={type.id} value={type.id}>
                {type.name}
              </option>
            ))}
          </NativeSelect>
        </div>
        <div className="flex min-w-0 flex-1 flex-col gap-1.5 md:w-45 md:flex-none">
          <Label htmlFor={`${id}-status`} className={label}>
            Situação
          </Label>
          <NativeSelect
            id={`${id}-status`}
            size="compact"
            value={filters.status}
            onChange={(event) => set({ status: event.target.value as StatusFilter })}
            className={cn(FILTER_FIELD, "pl-2.5 text-body-sm md:pl-3")}
          >
            {STATUS_OPTIONS.map(([value, text]) => (
              <option key={value} value={value}>
                {text}
              </option>
            ))}
          </NativeSelect>
        </div>
      </div>
      {children}
    </div>
  )
}

/**
 * Catálogo, aba Modelos (mockups 1, 2, 12 e 13). Todos os papéis consultam; "Novo modelo" só
 * aparece para quem edita (ADR-6), na barra dos filtros na web e fixo acima da barra inferior
 * no celular. O Técnico vê o aviso de que só consulta. No celular o título é o da barra da casca.
 */
function ModelsScreen({ canEdit }: { canEdit: boolean }) {
  const isDesktop = useMediaQuery(MD_UP)
  const [filters, setFilters] = React.useState(DEFAULT_FILTERS)
  const List = isDesktop ? WebList : MobileList

  return (
    <main
      className={cn(
        "flex flex-col gap-3 p-4 md:gap-5 md:p-8",
        // O botão fixo (72 px) fica sobre o fim da lista: a folga dele soma aos 16 px da página.
        canEdit && "max-md:pb-22",
      )}
    >
      <h1 className="text-display-lg max-md:sr-only">Catálogo</h1>
      <CatalogTabs active="models" />

      {!canEdit && (
        <p className="flex items-start gap-2 bg-chip-ok px-3 py-2.5 text-body-sm">
          <InfoIcon aria-hidden="true" className="mt-0.5 size-4.5 shrink-0" />
          Seu papel (Técnico) consulta o catálogo. Quem cadastra e altera é o
          Administrador e o Comercial.
        </p>
      )}

      <FilterBar filters={filters} onChange={setFilters} isDesktop={isDesktop}>
        {canEdit && (
          <div className="max-md:fixed max-md:inset-x-0 max-md:bottom-16 max-md:z-10 max-md:bg-ground max-md:px-4 max-md:py-3">
            <Button
              size="lg"
              nativeButton={false}
              render={<Link to="/catalogo/modelos/novo" />}
              role="link"
              className="w-full max-md:min-h-12 md:min-h-11 md:w-auto md:cut-sm md:px-5 md:text-body-sm md:font-semibold"
            >
              <PlusIcon aria-hidden="true" strokeWidth={2.5} className="size-4.5" />
              Novo modelo
            </Button>
          </div>
        )}
      </FilterBar>

      <section
        aria-label="Lista de modelos"
        className="min-w-0 md:overflow-x-auto md:border md:border-line md:bg-panel"
      >
        <div className="md:min-w-180">
          <List
            filters={filters}
            empty={
              <Empty
                filters={filters}
                canEdit={canEdit}
                onClear={() => setFilters(DEFAULT_FILTERS)}
              />
            }
          />
        </div>
      </section>
    </main>
  )
}

export { ModelsScreen }
