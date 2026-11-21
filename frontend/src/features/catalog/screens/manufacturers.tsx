import { PlusIcon } from "lucide-react"
import { Link, Outlet, useMatch, useParams } from "react-router"

import type { ApiError } from "@/shared/api/api-error"
import { cn } from "@/shared/lib/cn"
import { MD_UP, useMediaQuery } from "@/shared/lib/use-media-query"
import { Alert } from "@/shared/ui/alert"
import { Button } from "@/shared/ui/button"

import { useManufacturers } from "../catalog.hooks"
import { errorMessage } from "../catalog.messages"
import { CatalogTabs } from "../components/catalog-tabs"
import { ManufacturerRow, ManufacturerTableHeader } from "../components/manufacturer-row"
import { Spinner } from "../components/panel-frame"
import { MANUFACTURER_LIST, ManufacturerPanel } from "./manufacturer-panel"

/** Vazio (derivado: o mockup 7 só desenha a lista com fabricantes). */
function Empty({ canEdit }: { canEdit: boolean }) {
  return (
    <section
      aria-live="polite"
      className="mt-8 flex flex-col items-center gap-2.5 border border-line bg-panel px-4 py-6 text-center md:mt-0 md:border-0"
    >
      <h2 className="text-title">Nenhum fabricante cadastrado</h2>
      <p className="text-body-sm text-text-muted">
        {canEdit
          ? "Cadastre o primeiro."
          : "Quem cadastra é o Administrador e o Comercial."}
      </p>
    </section>
  )
}

/** Erro, carregando, vazio ou as linhas (com o cabeçalho na web). */
function List({ canEdit }: { canEdit: boolean }) {
  const query = useManufacturers(MANUFACTURER_LIST)
  const failure = query.error as Partial<ApiError> | null
  const manufacturers = query.data?.content

  return (
    <>
      {failure && (
        <div className="md:p-5">
          <Alert tone="danger">{errorMessage(failure)}</Alert>
        </div>
      )}

      {!manufacturers && !failure && <Spinner />}

      {manufacturers && manufacturers.length === 0 && <Empty canEdit={canEdit} />}

      {manufacturers && manufacturers.length > 0 && (
        <>
          <ManufacturerTableHeader />
          <ul className="flex flex-col gap-3 md:gap-0">
            {manufacturers.map((manufacturer) => (
              <ManufacturerRow
                key={manufacturer.id}
                manufacturer={manufacturer}
                canEdit={canEdit}
              />
            ))}
          </ul>
        </>
      )}
    </>
  )
}

/**
 * Catálogo, aba Fabricantes (mockups 7 e 8). Rota de layout de `/catalogo/fabricantes`, como
 * `usuarios`: na web a lista e o painel ficam lado a lado (sem filho, o painel é o "Novo
 * fabricante" do mockup; `novo` e `:id` o trocam pelo `Outlet`), e no celular os filhos são telas
 * cheias com cabeçalho próprio, no lugar da lista. A lista traz todas as situações: o desativado
 * continua ao alcance de Reativar. Sem `canEdit` (o Técnico, ADR-6) não há painel nem botão.
 */
function ManufacturersScreen({ canEdit }: { canEdit: boolean }) {
  const isDesktop = useMediaQuery(MD_UP)
  const { id } = useParams()
  // `novo` é rota irmã de `:id` (sem `id`): no celular também troca a lista pela tela.
  const creating = useMatch("/catalogo/fabricantes/novo") !== null
  const selected = id !== undefined || creating

  if (!isDesktop && selected) return <Outlet />

  return (
    <main
      className={cn(
        "flex flex-col gap-3 p-4 md:gap-5 md:p-8",
        // O botão fixo (72 px) fica sobre o fim da lista: a folga dele soma aos 16 px da página.
        canEdit && "max-md:pb-22",
      )}
    >
      <h1 className="text-display-lg max-md:sr-only">Catálogo</h1>
      <CatalogTabs active="manufacturers" />

      <div className="flex items-start gap-6">
        <section
          aria-label="Lista de fabricantes"
          className="min-w-0 flex-1 md:overflow-x-auto md:border md:border-line md:bg-panel"
        >
          <div className="md:min-w-120">
            <List canEdit={canEdit} />
          </div>
        </section>

        {/* Quem não edita não tem painel; a rota filha dele só redireciona para esta lista. */}
        {isDesktop && (selected || canEdit) && (
          <div className="w-85 shrink-0">
            {selected ? <Outlet /> : <ManufacturerPanel mode="new" canEdit={canEdit} />}
          </div>
        )}
      </div>

      {canEdit && (
        <div className="fixed inset-x-0 bottom-16 z-10 bg-ground px-4 py-3 md:hidden">
          <Button
            size="lg"
            nativeButton={false}
            render={<Link to="/catalogo/fabricantes/novo" />}
            role="link"
            className="min-h-12 w-full"
          >
            <PlusIcon aria-hidden="true" strokeWidth={2.5} className="size-4.5" />
            Novo fabricante
          </Button>
        </div>
      )}
    </main>
  )
}

export { ManufacturersScreen }
