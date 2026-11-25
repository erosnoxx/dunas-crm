import {
  ChevronLeftIcon,
  CircleAlertIcon,
  InfoIcon,
  LockIcon,
  type LucideIcon,
  MapPinIcon,
  PlusIcon,
  RotateCwIcon,
  WifiOffIcon,
} from "lucide-react"
import * as React from "react"
import { Link, useParams } from "react-router"

import type { ApiError } from "@/shared/api/api-error"
import { cn } from "@/shared/lib/cn"
import { MD_UP, useMediaQuery } from "@/shared/lib/use-media-query"
import { Alert } from "@/shared/ui/alert"
import { Button } from "@/shared/ui/button"
import { MobileHeader } from "@/shared/ui/mobile-header"
import { Spinner } from "@/shared/ui/spinner"

import {
  useCustomer,
  useDeactivateCustomer,
  useReactivateCustomer,
  useReactivateLocation,
} from "../customers.hooks"
import { errorMessage, locationsSummary } from "../customers.messages"
import type { CustomerDetail } from "../customers.service"
import { CustomerStatusChip } from "../components/customer-status"
import { DocumentText } from "../components/document-text"
import { LocationBlock } from "../components/location-block"
import { formatPhone } from "../phone"

const LIST_PATH = "/clientes"

// Textos aprovados das telas 11, 12 e 13; o do erro é o da tela 6 sem a frase da busca (só da lista).
const ROLE_NOTE =
  "Seu papel (Técnico) consulta os clientes. Quem cadastra e altera é o Administrador e o Comercial."
const DEACTIVATED_NOTE =
  "Cliente desativado: os dados dele não se editam e ele não recebe local nem equipamento novo até ser reativado. Os locais e os equipamentos aparecem como estão."
const EMPTY_TEXT =
  'Cadastre o endereço onde há instalação, como "Casa" ou "Fazenda Boa Vista". Os equipamentos são cadastrados dentro de cada local.'
const NETWORK_TEXT = "Sem resposta do servidor. Confira a conexão e tente de novo."

// `.open` das telas 9 e 10: link laranja de 44 px, 14 px 600.
const OPEN =
  "inline-flex min-h-11 items-center text-body-sm font-semibold text-brand-text"
// `.btn2` da tela 9 (linha 59): 44 px, `chip-ok`, 14 px 600, 20 de respiro, canto cortado.
const SECONDARY =
  "cut-sm inline-flex min-h-11 items-center bg-chip-ok px-5 text-body-sm font-semibold text-text"

type Failure = Partial<ApiError> | null

/**
 * A moldura da ficha. Na web: o link "Clientes" sobre o cabeçalho (tela 9, linhas 25 a 28). No
 * celular: o cabeçalho próprio "Cliente" (tela 10, linhas 17 a 20; a casca não põe as barras em
 * `/clientes/`) e a ação no rodapé fixo (linhas 55 e 56), com a folga dela no fim do conteúdo.
 */
function Frame({
  head,
  mobileAction,
  children,
}: {
  head?: React.ReactNode
  mobileAction?: React.ReactNode
  children: React.ReactNode
}) {
  const isDesktop = useMediaQuery(MD_UP)

  if (isDesktop)
    return (
      <main className="flex flex-col gap-5 p-8">
        <div>
          <Link
            to={LIST_PATH}
            className="inline-flex min-h-11 items-center gap-1.5 text-body-sm font-semibold text-text-muted"
          >
            <ChevronLeftIcon aria-hidden="true" strokeWidth={2.5} className="size-4.5" />
            Clientes
          </Link>
          {head}
        </div>
        {children}
      </main>
    )

  return (
    <>
      <MobileHeader to={LIST_PATH} title="Cliente" />
      <main
        className={cn("flex flex-col gap-4 px-4 pt-5", mobileAction ? "pb-24" : "pb-5")}
      >
        {children}
      </main>
      {mobileAction && (
        <div className="fixed inset-x-0 bottom-0 border-t border-line bg-nav px-4 py-3">
          {mobileAction}
        </div>
      )}
    </>
  )
}

/** O aviso `.ro` das telas 12 e 13: `chip-ok`, 14/20, ícone de 18. */
function Notice({ children }: { children: React.ReactNode }) {
  return (
    <p className="flex items-start gap-2 bg-chip-ok px-3 py-2.5 text-body-sm">
      <InfoIcon
        aria-hidden="true"
        strokeWidth={2.5}
        className="mt-0.5 size-4.5 shrink-0"
      />
      {children}
    </p>
  )
}

/**
 * Moldura do vazio (tela 11, linhas 35 a 37) e do erro (tela 6, linha 34): `panel` com contorno,
 * 10 px entre os itens; respiro 24/16 e ícone de 32 no celular, 48/24 e 40 na web.
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
  title: React.ReactNode
  role?: "alert"
  children?: React.ReactNode
}) {
  return (
    <section
      role={role}
      className="flex flex-col items-center gap-2.5 border border-line bg-panel px-4 py-6 text-center md:px-6 md:py-12"
    >
      <Icon aria-hidden="true" className={cn("size-8 md:size-10", iconClassName)} />
      {title}
      {children}
    </section>
  )
}

/** Falha ao carregar a ficha: o aviso da tela 6 (linhas 88 a 99), com "Tentar de novo". */
function LoadError({
  failure,
  onRetry,
}: {
  failure: Partial<ApiError>
  onRetry: () => void
}) {
  return (
    <StateFrame
      role="alert"
      icon={WifiOffIcon}
      iconClassName="text-danger"
      title={<h2 className="text-title">Não foi possível carregar o cliente</h2>}
    >
      <p className="flex max-w-140 items-start gap-1.5 text-left text-body-sm text-danger-text">
        <CircleAlertIcon
          aria-hidden="true"
          strokeWidth={2.5}
          className="mt-px size-4.5 shrink-0 text-danger"
        />
        {failure.status === 0 ? NETWORK_TEXT : errorMessage(failure)}
      </p>
      <button type="button" onClick={onRetry} className={cn(SECONDARY, "gap-2")}>
        <RotateCwIcon aria-hidden="true" strokeWidth={2.5} className="size-4.5" />
        Tentar de novo
      </button>
    </StateFrame>
  )
}

/**
 * Os dados (tela 9, linhas 30 a 36 e 110 a 118; tela 10, linhas 25 a 32 e 74 a 86): três colunas
 * na web, empilhados no celular. O cadeado só para quem edita o cliente ativo (as telas 12 e 13
 * não o têm); o ausente é "Não informado" (tela 11, linha 62).
 */
function CustomerData({
  customer,
  locked,
  links,
}: {
  customer: CustomerDetail
  locked: boolean
  links?: React.ReactNode
}) {
  const absent = <span className="text-text-muted">Não informado</span>
  const fields: [string, React.ReactNode][] = [
    [
      "Documento",
      <>
        <DocumentText type={customer.documentType} value={customer.document} badge />
        {locked && (
          <span className="mt-1 flex items-start gap-1.5 text-caption font-normal text-text-muted">
            <LockIcon
              aria-hidden="true"
              strokeWidth={2.5}
              className="size-3.5 shrink-0"
            />
            Não pode ser alterado.
          </span>
        )}
      </>,
    ],
    ["E-mail", customer.email ?? absent],
    ["Telefone", customer.phone === null ? absent : formatPhone(customer.phone)],
  ]

  return (
    <section
      aria-label="Dados do cliente"
      className="flex flex-col gap-3.5 border border-line bg-panel p-4 md:px-6 md:py-5"
    >
      <dl className="flex flex-col gap-3.5 md:grid md:grid-cols-3 md:gap-6">
        {fields.map(([label, value]) => (
          <div key={label} className="min-w-0">
            <dt className="text-label text-text-muted">{label}</dt>
            <dd className="text-body break-words md:mt-1">{value}</dd>
          </div>
        ))}
      </dl>
      {links && (
        <div className="flex items-center justify-between border-t border-line pt-1">
          {links}
        </div>
      )}
    </section>
  )
}

type Write = ReturnType<typeof useDeactivateCustomer>

/**
 * A ficha carregada (telas 9 a 13 e 16). Ativo, para quem edita: "Editar cliente" e "Desativar
 * cliente" (sem confirmação), "Novo local" e os locais com as ações deles. Desativado: o aviso e a
 * ação única "Reativar cliente"; os locais ativos seguem editáveis (ADR-30). Técnico: o aviso do
 * papel e nenhuma escrita (ADR-7). O erro de uma ação sai no aviso e a ficha se recarrega.
 */
function Detail({
  customer,
  canEdit,
  onReload,
}: {
  customer: CustomerDetail
  canEdit: boolean
  onReload: () => void
}) {
  const isDesktop = useMediaQuery(MD_UP)
  const deactivate = useDeactivateCustomer()
  const reactivate = useReactivateCustomer()
  const reactivateLocation = useReactivateLocation()
  const [failure, setFailure] = React.useState<Failure>(null)
  const active = customer.status === "ACTIVE"
  const editable = canEdit && active
  const { locations } = customer
  const activeCount = locations.filter((item) => item.status === "ACTIVE").length
  const newLocationPath = `${LIST_PATH}/${customer.id}/locais/novo`
  const size = isDesktop ? "md" : "lg"

  const run = (write: Write, id: string) => {
    setFailure(null)
    write.mutate(id, {
      onError: (e) => {
        setFailure(e as Partial<ApiError>)
        onReload()
      },
    })
  }

  const editLink = (className: string) => (
    <Link to={`${LIST_PATH}/${customer.id}/editar`} className={className}>
      Editar cliente
    </Link>
  )
  // `.dng` das telas 9 e 10: 44 px, `danger-text`, 14 px 600, sem confirmação (ADR-6).
  const deactivateButton = (
    <Button
      variant="danger-text"
      loading={deactivate.isPending}
      onClick={() => run(deactivate, customer.id)}
      className="text-body-sm"
    >
      Desativar cliente
    </Button>
  )
  // `.btn` da tela 12 (linha 53) na web; o rodapé de 48 px e 16 px da tela 10 (linha 56) no celular.
  const reactivateButton = (
    <Button
      size={size}
      loading={reactivate.isPending}
      onClick={() => run(reactivate, customer.id)}
      className={isDesktop ? "text-body-sm" : "min-h-12 w-full"}
    >
      Reativar cliente
    </Button>
  )
  const newLocation = (label: string) => (
    <Button
      size={size}
      nativeButton={false}
      render={<Link to={newLocationPath} />}
      role="link"
      className={isDesktop ? "ml-auto text-body-sm" : "min-h-12 w-full"}
    >
      <PlusIcon aria-hidden="true" strokeWidth={2.5} className="size-4.5" />
      {label}
    </Button>
  )

  const chip = <CustomerStatusChip status={customer.status} />
  // Web (tela 9, linhas 98 a 107): nome em `display-lg`, nome fantasia e selo 6 px abaixo, ações à
  // direita. Celular (tela 10, linhas 68 a 72): nome, nome fantasia e selo empilhados.
  const head = isDesktop ? (
    <div className="flex items-end justify-between gap-6">
      <div>
        <h1 className="text-display-lg">{customer.name}</h1>
        <div className="mt-1.5 flex items-center gap-3">
          {customer.tradeName && (
            <span className="text-body text-text-muted">{customer.tradeName}</span>
          )}
          {chip}
        </div>
      </div>
      {canEdit && (
        <div className="flex shrink-0 items-center gap-3">
          {active ? (
            <>
              {deactivateButton}
              {editLink(SECONDARY)}
            </>
          ) : (
            reactivateButton
          )}
        </div>
      )}
    </div>
  ) : (
    <div className="flex flex-col gap-1.5">
      <h2 className="text-title">{customer.name}</h2>
      {customer.tradeName && (
        <p className="text-body text-text-muted">{customer.tradeName}</p>
      )}
      <span className="self-start">{chip}</span>
    </div>
  )

  const notice = canEdit ? (
    !active && <Notice>{DEACTIVATED_NOTE}</Notice>
  ) : (
    <Notice>{ROLE_NOTE}</Notice>
  )

  const locationsBody =
    locations.length === 0 ? (
      <StateFrame
        icon={MapPinIcon}
        title={<h3 className="text-title">Nenhum local cadastrado</h3>}
      >
        {editable && (
          <>
            <p className="max-w-130 text-body-sm text-text-muted">{EMPTY_TEXT}</p>
            {/* No celular o convite é o rodapé fixo (tela 11, linhas 78 a 83). */}
            {isDesktop && (
              <Link to={newLocationPath} className={cn(OPEN, "justify-center px-3")}>
                Cadastrar o primeiro local
              </Link>
            )}
          </>
        )}
      </StateFrame>
    ) : (
      locations.map((location) => (
        <LocationBlock
          key={location.id}
          customerId={customer.id}
          location={location}
          customerActive={active}
          canEdit={canEdit}
          onReactivate={() => run(reactivateLocation, location.id)}
          reactivating={
            reactivateLocation.isPending && reactivateLocation.variables === location.id
          }
        />
      ))
    )

  const mobileAction =
    !isDesktop &&
    canEdit &&
    (active
      ? newLocation(locations.length === 0 ? "Cadastrar o primeiro local" : "Novo local")
      : reactivateButton)

  return (
    <Frame head={isDesktop && head} mobileAction={mobileAction}>
      {/* No celular o aviso do papel vem antes do nome (tela 13, linha 63). */}
      {!isDesktop && !canEdit && notice}
      {!isDesktop && head}
      {(isDesktop || canEdit) && notice}
      {failure && <Alert tone="danger">{errorMessage(failure)}</Alert>}
      <CustomerData
        customer={customer}
        locked={editable}
        links={
          !isDesktop &&
          editable && (
            <>
              {editLink(OPEN)}
              {deactivateButton}
            </>
          )
        }
      />
      {/* "Locais": tela 9, linhas 38 a 40 e 120 a 127; tela 10, linhas 35 a 37. */}
      <div
        className={
          isDesktop ? "flex items-center gap-3" : "mt-2 flex items-baseline gap-2.5"
        }
      >
        <h2 className="text-title">Locais</h2>
        {locations.length > 0 && (
          <span className="text-body-sm text-text-muted">
            {locationsSummary(activeCount, locations.length - activeCount)}
          </span>
        )}
        {isDesktop && editable && newLocation("Novo local")}
      </div>
      {locationsBody}
    </Frame>
  )
}

/**
 * `/clientes/:id`: a ficha do cliente, de todos os papéis; as ações só com `canEdit` (ADR-7).
 * `customer_not_found` sai no aviso; outra falha ao carregar, no aviso da tela 6 com "Tentar de novo".
 */
function CustomerDetailScreen({ canEdit }: { canEdit: boolean }) {
  const { id = "" } = useParams()
  const query = useCustomer(id)
  const customer = query.data
  const failure = query.error as Failure
  const reload = () => void query.refetch()

  if (customer) return <Detail customer={customer} canEdit={canEdit} onReload={reload} />

  return (
    <Frame>
      {!failure ? (
        <Spinner />
      ) : failure.code === "customer_not_found" ? (
        <Alert tone="danger">{errorMessage(failure)}</Alert>
      ) : (
        <LoadError failure={failure} onRetry={reload} />
      )}
    </Frame>
  )
}

export { CustomerDetailScreen }
