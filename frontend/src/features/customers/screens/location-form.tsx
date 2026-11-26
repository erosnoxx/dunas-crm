import { zodResolver } from "@hookform/resolvers/zod"
import * as React from "react"
import { type Resolver, useController, useForm } from "react-hook-form"
import { Link, Navigate, useNavigate, useParams } from "react-router"

import type { ApiError } from "@/shared/api/api-error"
import { cn } from "@/shared/lib/cn"
import { MD_UP, useMediaQuery } from "@/shared/lib/use-media-query"
import { Alert } from "@/shared/ui/alert"
import { Button } from "@/shared/ui/button"
import { FieldError } from "@/shared/ui/field-error"
import { Input } from "@/shared/ui/input"
import { Label } from "@/shared/ui/label"
import { NativeSelect } from "@/shared/ui/native-select"
import { Spinner } from "@/shared/ui/spinner"

import { CustomerStatusChip } from "../components/customer-status"
import { type Back, type Failure, Frame, WEB_BUTTON } from "../components/form-frame"
import {
  useCustomer,
  useDeactivateLocation,
  useReactivateLocation,
  useRegisterLocation,
  useUpdateLocation,
} from "../customers.hooks"
import { errorMessage } from "../customers.messages"
import { locationSchema, type LocationValues } from "../customers.schemas"
import {
  type CustomerDetail,
  type LocationInput,
  findZipCode,
  type LocationView,
  UFS,
} from "../customers.service"
import { describedBy } from "../display"
import { formatZipCode, maskZipCode } from "../zip-code"

const customerPath = (id: string) => `/clientes/${id}`

// Limites do `Address` e do `LocationName` (domain-model.md); os de número, complemento e cidade
// não têm mensagem: o campo não aceita o caractere a mais (usecases.md, "Mensagens da tela").
const MAX = {
  name: 60,
  street: 150,
  number: 15,
  complement: 50,
  neighborhood: 100,
  city: 100,
} as const

// Textos aprovados da tela 14 (linhas 86, 101 e 112); o do local desativado é o da tela 16.
const NAME_HELP =
  'Um nome curto para achar o local na ficha, como "Casa" ou "Fazenda Boa Vista".'
const ZIP_CODE_HELP = "Ao digitar o CEP, preenchemos o endereço."
// Resultado da consulta do CEP (ADR-34), sob o campo, no celular também.
const LOOKUP_TEXT = {
  loading: "Buscando o endereço…",
  found: "Endereço preenchido. Cidade e UF vêm do CEP e não se alteram.",
  missing: "Não achamos este CEP. Preencha o endereço à mão.",
  unavailable: "Não deu para consultar o CEP agora. Preencha o endereço à mão.",
} as const
type Lookup = "idle" | keyof typeof LOOKUP_TEXT
const NUMBER_HELP = 'Sem número, use "S/N".'
const DEACTIVATED_NOTE =
  "Local desativado: não recebe equipamento novo e não se edita até ser reativado. Os equipamentos dele ficam como estão."
// Sem artboard (D3): a nota da tela 16 no molde da ajuda do modelo do catálogo.
const DEACTIVATE_HELP =
  "Desativar marca o local na ficha: ele não recebe equipamento novo e não se edita até ser reativado. Os equipamentos dele ficam como estão. Dá para reativar a qualquer momento."

// Painel da tela 14 (linha 28): `panel`, contorno `line`, 24 px de respiro, 18 entre os blocos,
// 640 px que encolhem com a tela; no celular (tela 15, linha 20), 14 entre os blocos, sem painel.
const FORM =
  "flex flex-col gap-3.5 md:w-160 md:max-w-full md:gap-4.5 md:border md:border-line md:bg-panel md:p-6"
const FIELD = "flex flex-col gap-1.5"
// `.help` (tela 14, linha 33): 12/16 `text-muted`, no peso do corpo; só na web, como na tela 15.
const HELP = "hidden text-caption font-normal text-text-muted md:block"
// O painel "Situação" do modelo no catálogo (Modelo-web.dc.html, linhas 28, 29 e 104): 24 de
// respiro e 20 entre os itens, 340 px, ao lado do formulário com 24 entre os dois.
const PANEL = "flex flex-col gap-3.5 border border-line bg-panel p-4 md:gap-5 md:p-6"
// O formulário guarda texto: a UF vazia é o "nenhuma escolhida" que o schema recusa.
type LocationFields = Record<keyof LocationValues, string>

const resolveLocation = zodResolver(locationSchema) as Resolver<
  LocationFields,
  unknown,
  LocationValues
>

const EMPTY: LocationFields = {
  name: "",
  zipCode: "",
  street: "",
  number: "",
  complement: "",
  neighborhood: "",
  city: "",
  state: "",
}

const fieldsOf = (location: LocationView): LocationFields => ({
  name: location.name,
  ...location.address,
  zipCode: formatZipCode(location.address.zipCode),
  complement: location.address.complement ?? "",
})

const customerText = (customer: CustomerDetail) =>
  customer.tradeName ? `${customer.name} (${customer.tradeName})` : customer.name

const backTo = (customer: CustomerDetail): Back => ({
  to: customerPath(customer.id),
  label: customer.name,
})

/**
 * O formulário do local (novo e edição). O CEP vai com os 8 dígitos e o complemento vazio como
 * `null` (ADR-19, ADR-31). Nenhum erro do servidor tem campo nesta tela: todos saem no aviso
 * (usecases.md, "Onde aparece").
 */
function useLocationForm(
  defaults: LocationFields,
  send: (input: LocationInput) => Promise<void>,
) {
  const [failure, setFailure] = React.useState<Failure>(null)
  const form = useForm<LocationFields, unknown, LocationValues>({
    defaultValues: defaults,
    resolver: resolveLocation,
  })

  const submit = form.handleSubmit(async ({ name, ...address }) => {
    setFailure(null)

    try {
      await send({
        name,
        address: {
          ...address,
          zipCode: address.zipCode.replace(/\D/g, ""),
          complement: address.complement || null,
        },
      })
    } catch (e) {
      setFailure(e as Partial<ApiError>)
    }
  })

  const [lookup, setLookup] = React.useState<Lookup>("idle")
  // A resposta de um CEP já trocado não escreve no formulário; `filled` lembra se o logradouro e o
  // bairro atuais vieram de uma consulta, para limpá-los quando o CEP novo não os traz.
  const latest = React.useRef("")
  const filled = React.useRef(false)

  /** ADR-34: com 8 dígitos busca o endereço; achado, cidade e UF travam até o CEP mudar. */
  const lookUp = (digits: string) => {
    latest.current = digits

    if (digits.length < 8) return setLookup("idle")

    setLookup("loading")
    findZipCode(digits).then(
      (address) => {
        if (latest.current !== digits) return

        const fill = (name: keyof LocationFields, value: string) =>
          form.setValue(name, value, { shouldDirty: true, shouldValidate: true })

        if (address.street ?? filled.current) fill("street", address.street ?? "")
        if (address.neighborhood ?? filled.current)
          fill("neighborhood", address.neighborhood ?? "")
        fill("city", address.city)
        fill("state", address.state)
        filled.current = true
        setLookup("found")
        form.setFocus("number")
      },
      (e: Partial<ApiError>) => {
        if (latest.current !== digits) return

        setLookup(e.code === "zip_code_not_found" ? "missing" : "unavailable")
      },
    )
  }

  return { form, failure, setFailure, submit, lookup, lookUp }
}

type LocationForm = ReturnType<typeof useLocationForm>
type TextName = Exclude<keyof LocationFields, "zipCode" | "state">

function TextField({
  model,
  name,
  label,
  help,
  placeholder,
  locked,
}: {
  model: LocationForm
  name: TextName
  label: string
  help?: string
  placeholder?: string
  /** Preenchido pelo CEP: só leitura. */
  locked?: boolean
}) {
  const id = React.useId()
  const error = model.form.formState.errors[name]?.message

  return (
    <div className={FIELD}>
      <Label htmlFor={id}>{label}</Label>
      <Input
        id={id}
        size="compact"
        type="text"
        autoComplete="off"
        maxLength={MAX[name]}
        readOnly={locked}
        placeholder={placeholder}
        className={locked ? "bg-nav text-text-muted" : undefined}
        aria-invalid={error ? true : undefined}
        aria-describedby={describedBy(error && `${id}-error`, help && `${id}-help`)}
        {...model.form.register(name)}
      />
      {error && <FieldError id={`${id}-error`}>{error}</FieldError>}
      {help && (
        <p id={`${id}-help`} className={HELP}>
          {help}
        </p>
      )}
    </div>
  )
}

/** O CEP com a máscara `00000-000` (ADR-19), 160 px na tela 15 (linha 54) e na coluna da 14. */
function ZipCodeField({ model, helpId }: { model: LocationForm; helpId: string }) {
  const id = React.useId()
  const {
    field: { ref, name, value, onBlur, onChange },
    fieldState,
  } = useController({ control: model.form.control, name: "zipCode" })
  const error = fieldState.error?.message

  return (
    <div className={FIELD}>
      <Label htmlFor={id}>CEP *</Label>
      <Input
        id={id}
        size="compact"
        type="text"
        inputMode="numeric"
        autoComplete="off"
        placeholder="00000-000"
        className="w-40"
        aria-invalid={error ? true : undefined}
        aria-describedby={describedBy(error && `${id}-error`, helpId)}
        ref={ref}
        name={name}
        value={value}
        onBlur={onBlur}
        onChange={(event) => {
          const masked = maskZipCode(event.target.value)

          onChange(masked)
          model.lookUp(masked.replace(/\D/g, ""))
        }}
      />
      {error && <FieldError id={`${id}-error`}>{error}</FieldError>}
    </div>
  )
}

/** A UF em lista fechada com as 27 siglas (tela 14, linha 124); a vazia é "nenhuma escolhida". */
function StateField({ model, locked }: { model: LocationForm; locked: boolean }) {
  const id = React.useId()
  const error = model.form.formState.errors.state?.message

  return (
    <div className={FIELD}>
      <Label htmlFor={id}>UF *</Label>
      <NativeSelect
        id={id}
        size="compact"
        disabled={locked}
        aria-invalid={error ? true : undefined}
        aria-describedby={error ? `${id}-error` : undefined}
        {...model.form.register("state")}
      >
        <option value="" disabled />
        {UFS.map((uf) => (
          <option key={uf} value={uf}>
            {uf}
          </option>
        ))}
      </NativeSelect>
      {error && <FieldError id={`${id}-error`}>{error}</FieldError>}
    </div>
  )
}

/**
 * Os campos, na ordem das telas 14 e 15. Web: CEP e logradouro, número e complemento lado a lado
 * (160 px e o resto), bairro, cidade e UF (96 px) na mesma linha, cada dica sob a sua linha.
 * Celular: o CEP sozinho, número e complemento meio a meio, o bairro sozinho, cidade e UF (92 px).
 */
function LocationFields({
  model,
  id,
  customer,
  top,
  footer,
}: {
  model: LocationForm
  id: string
  customer: CustomerDetail
  top?: React.ReactNode
  footer?: React.ReactNode
}) {
  const isDesktop = useMediaQuery(MD_UP)
  const helpId = React.useId()
  const locked = model.lookup === "found"

  return (
    <form id={id} noValidate onSubmit={model.submit} className={FORM}>
      {model.failure && <Alert tone="danger">{errorMessage(model.failure)}</Alert>}
      {top}
      {isDesktop ? (
        <div className={FIELD}>
          <span className="text-label">Cliente</span>
          <div className="flex h-11 items-center border border-line bg-nav px-3 text-body text-text-muted">
            <span className="truncate">{customerText(customer)}</span>
          </div>
        </div>
      ) : (
        <p className="text-body-sm text-text-muted">Cliente: {customerText(customer)}</p>
      )}
      <TextField model={model} name="name" label="Nome do local *" help={NAME_HELP} />
      <section
        aria-labelledby={`${helpId}-title`}
        className="flex flex-col gap-3 border-t border-line pt-4 md:pt-4.5"
      >
        <h2 id={`${helpId}-title`} className="text-label text-text-muted">
          Endereço
        </h2>
        <div className="flex flex-col gap-3 md:grid md:grid-cols-[--spacing(40)_minmax(0,1fr)]">
          <ZipCodeField model={model} helpId={`${helpId}-zip`} />
          <TextField model={model} name="street" label="Logradouro *" />
        </div>
        {/* Aviso vivo: o leitor de tela ouve o resultado da consulta sem sair do CEP. */}
        <p role="status" className="text-caption text-text-muted empty:hidden">
          {model.lookup !== "idle" && LOOKUP_TEXT[model.lookup]}
        </p>
        <p id={`${helpId}-zip`} className={cn(HELP, "-mt-1.5")}>
          {ZIP_CODE_HELP}
        </p>
        <div className="grid grid-cols-2 gap-3 md:grid-cols-[--spacing(40)_minmax(0,1fr)]">
          <TextField model={model} name="number" label="Número *" help={NUMBER_HELP} />
          <TextField
            model={model}
            name="complement"
            label={isDesktop ? "Complemento (opcional)" : "Complemento"}
            placeholder={isDesktop ? undefined : "Opcional"}
          />
        </div>
        <div className="grid grid-cols-[minmax(0,1fr)_--spacing(23)] gap-3 md:grid-cols-[minmax(0,1fr)_minmax(0,1fr)_--spacing(24)]">
          <div className="col-span-2 md:col-span-1">
            <TextField model={model} name="neighborhood" label="Bairro *" />
          </div>
          <TextField model={model} name="city" label="Cidade *" locked={locked} />
          <StateField model={model} locked={locked} />
        </div>
      </section>
      {footer}
    </form>
  )
}

/** A página do formulário: o salvar no rodapé fixo no celular e ao lado de Cancelar na web. */
function FormPage({
  customer,
  title,
  saveLabel,
  model,
  top,
  mobileFooter,
  aside,
}: {
  customer: CustomerDetail
  title: string
  saveLabel: string
  model: LocationForm
  top?: React.ReactNode
  mobileFooter?: React.ReactNode
  aside?: React.ReactNode
}) {
  const isDesktop = useMediaQuery(MD_UP)
  const formId = React.useId()
  const back = backTo(customer)
  const save = (
    <Button
      type="submit"
      form={formId}
      size={isDesktop ? "md" : "lg"}
      loading={model.form.formState.isSubmitting}
      className={isDesktop ? WEB_BUTTON : "min-h-12 w-full"}
    >
      {saveLabel}
    </Button>
  )
  const fields = (
    <LocationFields
      model={model}
      id={formId}
      customer={customer}
      top={!isDesktop && top}
      footer={
        isDesktop ? (
          // "Cancelar" e o salvar (tela 14, linhas 38, 40, 130 e 131).
          <div className="flex justify-end gap-3">
            <Link
              to={back.to}
              className="cut-sm inline-flex min-h-11 items-center bg-chip-ok px-5 text-body-sm font-semibold text-text"
            >
              Cancelar
            </Link>
            {save}
          </div>
        ) : (
          mobileFooter
        )
      }
    />
  )

  return (
    <Frame back={back} title={title} mobileAction={!isDesktop && save}>
      {isDesktop && aside ? (
        <div className="flex flex-col gap-6 xl:flex-row xl:items-start">
          {fields}
          {aside}
        </div>
      ) : (
        fields
      )}
    </Frame>
  )
}

/** Novo local (telas 14 e 15): salvar volta para a ficha. */
function NewLocation({ customer }: { customer: CustomerDetail }) {
  const navigate = useNavigate()
  const register = useRegisterLocation()
  const model = useLocationForm(EMPTY, async (input) => {
    await register.mutateAsync({ customerId: customer.id, input })
    void navigate(customerPath(customer.id))
  })

  return (
    <FormPage
      customer={customer}
      title="Novo local"
      saveLabel="Salvar local"
      model={model}
    />
  )
}

/** O painel "Situação" ao lado, na web (D3, D6), como o do modelo do catálogo. */
function StatusAside({ children }: { children: React.ReactNode }) {
  return (
    <aside
      aria-label="Situação do local"
      className={cn(PANEL, "md:max-w-160 xl:w-85 xl:shrink-0")}
    >
      <h2 className="text-label text-text-muted">Situação</h2>
      {children}
    </aside>
  )
}

const chip = (location: LocationView) => (
  <span className="self-start">
    <CustomerStatusChip status={location.status} />
  </span>
)

/**
 * Editar local (D3 na web, D4 no celular): os valores do próprio local, o nome dele no título e
 * "Salvar alterações", que volta para a ficha. "Desativar local" sem confirmação (ADR-6, ADR-32):
 * a ficha se recarrega e a página passa ao local desativado sem sair dela.
 */
function EditLocation({
  customer,
  location,
}: {
  customer: CustomerDetail
  location: LocationView
}) {
  const navigate = useNavigate()
  const update = useUpdateLocation()
  const deactivate = useDeactivateLocation()
  const model = useLocationForm(fieldsOf(location), async (input) => {
    await update.mutateAsync({ id: location.id, input })
    void navigate(customerPath(customer.id))
  })
  const onDeactivate = () => {
    model.setFailure(null)
    deactivate.mutate(location.id, {
      onError: (e) => model.setFailure(e as Partial<ApiError>),
    })
  }

  return (
    <FormPage
      customer={customer}
      title={location.name}
      saveLabel="Salvar alterações"
      model={model}
      top={chip(location)}
      // `.dng` da tela 19 (linha 37): 44 px, `danger-text`, 14 px 600, linha em cima, 6 de respiro.
      mobileFooter={
        <Button
          variant="danger-text"
          loading={deactivate.isPending}
          onClick={onDeactivate}
          className="w-full justify-start border-t border-line pt-1.5 text-body-sm"
        >
          Desativar local
        </Button>
      }
      aside={
        <StatusAside>
          {chip(location)}
          <p className="text-body-sm text-text-muted">{DEACTIVATE_HELP}</p>
          {/* `.dng` do modelo (Modelo-web.dc.html, linha 44). */}
          <Button
            variant="danger-text"
            loading={deactivate.isPending}
            onClick={onDeactivate}
            className="min-h-11 self-start text-body-sm"
          >
            Desativar local
          </Button>
        </StatusAside>
      }
    />
  )
}

/**
 * Local desativado (D6): só leitura, como o estado desativado da tela 19 (linhas 82 a 97), com a
 * ação única "Reativar local"; reativar recarrega a ficha e a página volta ao formulário.
 */
function DeactivatedLocation({
  customer,
  location,
}: {
  customer: CustomerDetail
  location: LocationView
}) {
  const isDesktop = useMediaQuery(MD_UP)
  const reactivate = useReactivateLocation()
  const [failure, setFailure] = React.useState<Failure>(null)
  const { address } = location
  const absent = <span className="text-text-muted">Não informado</span>
  const fields: [string, React.ReactNode][] = [
    ["Cliente", customerText(customer)],
    ["CEP", formatZipCode(address.zipCode)],
    ["Logradouro", address.street],
    ["Número", address.number],
    ["Complemento", address.complement ?? absent],
    ["Bairro", address.neighborhood],
    ["Cidade", address.city],
    ["UF", address.state],
  ]

  const note = <p className="text-body-sm text-text-muted">{DEACTIVATED_NOTE}</p>
  const action = (
    <Button
      size={isDesktop ? "md" : "lg"}
      loading={reactivate.isPending}
      onClick={() => {
        setFailure(null)
        reactivate.mutate(location.id, {
          onError: (e) => setFailure(e as Partial<ApiError>),
        })
      }}
      className={isDesktop ? cn(WEB_BUTTON, "self-start") : "min-h-12 w-full"}
    >
      Reativar local
    </Button>
  )
  // `.panel`, `.k` e `.v` da tela 19 (linhas 32 a 34 e 85 a 90).
  const data = (
    <section aria-label="Dados do local" className={cn(PANEL, "md:w-160 md:max-w-full")}>
      <dl className="flex flex-col gap-3.5 md:gap-5">
        {fields.map(([key, value]) => (
          <div key={key}>
            <dt className="text-label text-text-muted">{key}</dt>
            <dd className="text-body break-words">{value}</dd>
          </div>
        ))}
      </dl>
    </section>
  )
  const alert = failure && <Alert tone="danger">{errorMessage(failure)}</Alert>

  if (isDesktop)
    return (
      <Frame back={backTo(customer)} title={location.name}>
        {alert}
        <div className="flex flex-col gap-6 xl:flex-row xl:items-start">
          {data}
          <StatusAside>
            {chip(location)}
            {note}
            {action}
          </StatusAside>
        </div>
      </Frame>
    )

  return (
    <Frame back={backTo(customer)} title={location.name} mobileAction={action}>
      {/* A tela 19 desligada tem 18 entre os blocos (linha 20). */}
      <div className="flex flex-col gap-4.5">
        {alert}
        {chip(location)}
        {data}
        {note}
      </div>
    </Frame>
  )
}

const LOADING_BACK = (id: string): Back => ({
  to: customerPath(id),
  label: "Ficha do cliente",
})

/**
 * Lê a ficha (sem `GET` do local: ADR-22) e só monta a página com ela lida agora (a do cache pode
 * ser de antes da última escrita): até lá não há campo nem ação. O local que não está na ficha
 * (de outro cliente ou inexistente) é "local não encontrado". Local novo só com o cliente ativo
 * (ADR-6); o local ativo de cliente desativado se edita (ADR-30).
 */
function LocationLoader({
  mode,
  id,
  locationId,
}: {
  mode: "new" | "edit"
  id: string
  locationId: string
}) {
  const query = useCustomer(id)
  const customer = query.data
  const failure = query.error as Failure

  if (failure || !customer || !query.isFetchedAfterMount)
    return (
      <Frame back={LOADING_BACK(id)}>
        {failure ? <Alert tone="danger">{errorMessage(failure)}</Alert> : <Spinner />}
      </Frame>
    )

  if (mode === "new")
    return customer.status === "ACTIVE" ? (
      <NewLocation customer={customer} />
    ) : (
      <Navigate to={customerPath(id)} replace />
    )

  const location = customer.locations.find((item) => item.id === locationId)

  if (!location)
    return (
      <Frame back={backTo(customer)}>
        <Alert tone="danger">{errorMessage({ code: "location_not_found" })}</Alert>
      </Frame>
    )

  if (location.status === "DEACTIVATED")
    return <DeactivatedLocation customer={customer} location={location} />

  return <EditLocation key={location.id} customer={customer} location={location} />
}

/**
 * `new` (telas 14 e 15) e `edit` (D3, D4, D6) do local. Sem `canEdit` (o Técnico, ADR-7) as duas
 * voltam para a ficha. Trocar de local ou de cliente remonta tudo: a ficha é lida de novo e o
 * formulário não guarda o estado do anterior.
 */
function LocationFormScreen({
  mode,
  canEdit,
}: {
  mode: "new" | "edit"
  canEdit: boolean
}) {
  const { id = "", locationId = "" } = useParams()

  if (!canEdit) return <Navigate to={customerPath(id)} replace />

  return (
    <LocationLoader
      key={`${id}/${locationId}`}
      mode={mode}
      id={id}
      locationId={locationId}
    />
  )
}

export { LocationFormScreen }
