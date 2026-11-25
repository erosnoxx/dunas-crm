import { zodResolver } from "@hookform/resolvers/zod"
import { LockIcon } from "lucide-react"
import * as React from "react"
import { useForm } from "react-hook-form"
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
  useDeactivateInstalledEquipment,
  useInstallEquipment,
  useModelOptions,
  useReactivateInstalledEquipment,
  useUpdateInstalledEquipment,
} from "../customers.hooks"
import { errorMessage, LABELS } from "../customers.messages"
import { equipmentSchema, type EquipmentValues } from "../customers.schemas"
import type {
  CustomerDetail,
  EquipmentInput,
  EquipmentView,
  LocationView,
  ModelOption,
  Unit,
} from "../customers.service"
import { describedBy, formatAmount, formatDate } from "../display"

const customerPath = (id: string) => `/clientes/${id}`

// Limite do `SerialNumber` (ADR-20): sem mensagem, o campo não aceita o caractere a mais.
const SERIAL_MAX = 60
const TAKEN = "serial_number_taken"

// Textos aprovados: tela 17 (linhas 94, 100, 105 e 110), tela 18 (linhas 53 e 60) e tela 19
// (linhas 62, 72 e 91).
const MODEL_HELP =
  "Só modelos ativos do catálogo, com fabricante, tipo e característica para distinguir. Não achou? Cadastre em Catálogo, Modelos."
const MODEL_HELP_SHORT = "Só modelos ativos do catálogo."
const SERIAL_HELP = "Copie da etiqueta do equipamento."
const FIX_SERIAL_HELP = "Corrija só erro de digitação: a série é a da etiqueta."
const DATE_HELP = "Não pode ser depois de hoje."
const NEW_LOCK =
  "Modelo e local não mudam depois de salvos. A série e a data podem ser corrigidas."
const EDIT_LOCK =
  "Modelo e local não mudam depois de salvos. Errou? Desative e cadastre de novo."
const TAKEN_HINT = "Confira a etiqueta ou busque a série na lista de clientes."
const DEACTIVATED_NOTE =
  "Este equipamento está desativado e continua na ficha, marcado. A série dele segue ocupada para este modelo. Para corrigir a série ou a data, reative primeiro."
// Sem artboard (D5): a nota da tela 19 no molde da ajuda do modelo do catálogo.
const DEACTIVATE_HELP =
  "Desativar marca o equipamento na ficha: ele continua nela e a série dele segue ocupada para este modelo. Dá para reativar a qualquer momento."

// Painel da tela 17 (linha 28): `panel`, contorno `line`, 24 px de respiro, 20 entre os blocos,
// 640 px que encolhem com a tela; no celular (telas 18 e 19, linha 20), 18 entre os blocos.
const FORM =
  "flex flex-col gap-4.5 md:w-160 md:max-w-full md:gap-5 md:border md:border-line md:bg-panel md:p-6"
const FIELD = "flex flex-col gap-1.5"
// `.help` e `.lock` (tela 17, linha 33; tela 19, linha 30): 12/16 `text-muted`, no peso do corpo.
const HELP = "text-caption font-normal text-text-muted"
const WEB_HELP = cn(HELP, "hidden md:block")
// `.ro` (tela 17, linha 32; tela 18, linha 25): 44 e 48 px, fundo `nav`, contorno `line`, `text-muted`.
const READ_ONLY =
  "flex min-h-12 items-center border border-line bg-nav px-3 text-body break-words text-text-muted md:min-h-11"
// `.panel` da tela 19 (linha 32) no celular; o painel "Situação" do modelo no catálogo na web.
const PANEL = "flex flex-col gap-3.5 border border-line bg-panel p-4 md:gap-5 md:p-6"

type EquipmentFields = { modelId: string; serialNumber: string; installedOn: string }
type Value = { unit: Unit; amount: string | number }

// O `sv-SE` escreve a data local como `yyyy-MM-dd`, o formato do `input type="date"` e da API.
const today = () => new Date().toLocaleDateString("sv-SE")

/** "SUN2000-5KTL-L1 · Huawei · Inversor · 5 kW" (tela 17, linha 92): o valor sem milhar. */
const modelText = (name: string, manufacturer: string, type: string, values: Value[]) =>
  [
    name,
    manufacturer,
    type,
    ...values.map((value) => `${formatAmount(value.amount)} ${LABELS.unit[value.unit]}`),
  ].join(" · ")

const optionText = (option: ModelOption) =>
  modelText(option.name, option.manufacturer.name, option.type.name, option.values)

const equipmentText = (item: EquipmentView) =>
  modelText(
    item.model.name,
    item.manufacturer.name,
    item.type.name,
    item.characteristic ? [item.characteristic] : [],
  )

/** O local do equipamento salvo, com o cliente: "Loja Centro · Padaria Boa Vista" (tela 19, linha 59). */
const placeText = (customer: CustomerDetail, location: LocationView) =>
  `${location.name} · ${customer.tradeName ?? customer.name}`

const backTo = (customer: CustomerDetail): Back => ({
  to: customerPath(customer.id),
  label: customer.name,
})

/**
 * O formulário do equipamento (novo e edição). A data vazia vai como `null` (ADR-31). Os erros do
 * servidor saem pelo `code`: a série repetida sob a série (no cadastro, com a dica da tela 18), a
 * data no futuro sob a data; o resto, no aviso (usecases.md, "Onde aparece").
 */
function useEquipmentForm(
  defaults: EquipmentFields,
  send: (input: EquipmentInput) => Promise<void>,
  takenHint?: string,
) {
  const [failure, setFailure] = React.useState<Failure>(null)
  const [max] = React.useState(today)
  const form = useForm<EquipmentFields, unknown, EquipmentValues>({
    defaultValues: defaults,
    resolver: zodResolver(equipmentSchema(max)),
  })

  const submit = form.handleSubmit(async (values) => {
    setFailure(null)

    try {
      await send({ ...values, installedOn: values.installedOn || null })
    } catch (e) {
      const error = e as Partial<ApiError>
      const message = errorMessage(error)

      if (error.code === TAKEN) {
        form.setError("serialNumber", {
          message: takenHint ? `${message} ${takenHint}` : message,
        })
        form.setFocus("serialNumber")
        return
      }

      if (error.code === "invalid_installation_date") {
        form.setError("installedOn", { message })
        form.setFocus("installedOn")
        return
      }

      setFailure(error)
    }
  })

  return { form, failure, setFailure, submit, max }
}

type EquipmentForm = ReturnType<typeof useEquipmentForm>

function Lock({ className, children }: { className?: string; children: string }) {
  return (
    <p className={cn(HELP, "flex items-start gap-1.5", className)}>
      <LockIcon aria-hidden="true" strokeWidth={2.5} className="size-3.5 shrink-0" />
      {children}
    </p>
  )
}

function ReadOnly({
  label,
  children,
  note,
}: {
  label: string
  children: string
  note?: React.ReactNode
}) {
  return (
    <div className={FIELD}>
      <span className="text-label">{label}</span>
      <div className={READ_ONLY}>{children}</div>
      {note}
    </div>
  )
}

/** O modelo em lista fechada (tela 17, linhas 84 a 95); a vazia é "nenhum escolhido". */
function ModelField({
  model,
  options,
}: {
  model: EquipmentForm
  options: ModelOption[] | undefined
}) {
  const id = React.useId()
  const isDesktop = useMediaQuery(MD_UP)
  const error = model.form.formState.errors.modelId?.message

  return (
    <div className={FIELD}>
      <Label htmlFor={id}>Modelo *</Label>
      <NativeSelect
        id={id}
        size="compact"
        disabled={!options}
        aria-invalid={error ? true : undefined}
        aria-describedby={describedBy(error && `${id}-error`, `${id}-help`)}
        {...model.form.register("modelId")}
      >
        <option value="" disabled />
        {options?.map((option) => (
          <option key={option.id} value={option.id}>
            {optionText(option)}
          </option>
        ))}
      </NativeSelect>
      {error && <FieldError id={`${id}-error`}>{error}</FieldError>}
      <p id={`${id}-help`} className={HELP}>
        {isDesktop ? MODEL_HELP : MODEL_HELP_SHORT}
      </p>
    </div>
  )
}

/**
 * Série e data, lado a lado na web (tela 17, linhas 35 e 96 a 107: o resto e 220 px) e empilhadas
 * no celular (telas 18 e 19). As dicas que o celular não desenha só aparecem na web.
 */
function SerialAndDate({
  model,
  serialHelp,
  serialHelpClass,
}: {
  model: EquipmentForm
  serialHelp: string
  serialHelpClass: string
}) {
  const id = React.useId()
  const { errors } = model.form.formState
  const serialError = errors.serialNumber?.message
  const dateError = errors.installedOn?.message

  return (
    <div className="flex flex-col gap-4.5 md:grid md:grid-cols-[minmax(0,1fr)_--spacing(55)] md:gap-3">
      <div className={FIELD}>
        <Label htmlFor={`${id}-serial`}>Número de série *</Label>
        <Input
          id={`${id}-serial`}
          size="compact"
          type="text"
          autoComplete="off"
          maxLength={SERIAL_MAX}
          aria-invalid={serialError ? true : undefined}
          aria-describedby={describedBy(
            serialError && `${id}-serial-error`,
            `${id}-serial-help`,
          )}
          {...model.form.register("serialNumber")}
        />
        {serialError && <FieldError id={`${id}-serial-error`}>{serialError}</FieldError>}
        <p id={`${id}-serial-help`} className={serialHelpClass}>
          {serialHelp}
        </p>
      </div>
      <div className={FIELD}>
        <Label htmlFor={`${id}-date`}>Data de instalação (opcional)</Label>
        <Input
          id={`${id}-date`}
          size="compact"
          type="date"
          max={model.max}
          aria-invalid={dateError ? true : undefined}
          aria-describedby={describedBy(
            dateError && `${id}-date-error`,
            `${id}-date-help`,
          )}
          {...model.form.register("installedOn")}
        />
        {dateError && <FieldError id={`${id}-date-error`}>{dateError}</FieldError>}
        <p id={`${id}-date-help`} className={WEB_HELP}>
          {DATE_HELP}
        </p>
      </div>
    </div>
  )
}

/**
 * A página do formulário: os campos fixos do modo (`head`), série e data, o que vem depois
 * (`tail`) e o salvar no rodapé fixo no celular ou ao lado de Cancelar na web.
 */
function FormPage({
  customer,
  title,
  saveLabel,
  model,
  head,
  serialHelp,
  serialHelpClass,
  tail,
  alerts,
  saveDisabled,
  mobileFooter,
  aside,
}: {
  customer: CustomerDetail
  title: string
  saveLabel: string
  model: EquipmentForm
  head: React.ReactNode
  serialHelp: string
  serialHelpClass: string
  tail?: React.ReactNode
  alerts?: React.ReactNode
  saveDisabled?: boolean
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
      disabled={saveDisabled}
      loading={model.form.formState.isSubmitting}
      className={isDesktop ? WEB_BUTTON : "min-h-12 w-full"}
    >
      {saveLabel}
    </Button>
  )
  const form = (
    <form id={formId} noValidate onSubmit={model.submit} className={FORM}>
      {model.failure && <Alert tone="danger">{errorMessage(model.failure)}</Alert>}
      {alerts}
      {head}
      <SerialAndDate
        model={model}
        serialHelp={serialHelp}
        serialHelpClass={serialHelpClass}
      />
      {tail}
      {isDesktop ? (
        // "Cancelar" e o salvar (tela 17, linhas 36 a 38 e 113 a 116).
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
      )}
    </form>
  )

  return (
    <Frame back={back} title={title} mobileAction={!isDesktop && save}>
      {isDesktop && aside ? (
        <div className="flex items-start gap-6">
          {form}
          {aside}
        </div>
      ) : (
        form
      )}
    </Frame>
  )
}

const EMPTY: EquipmentFields = { modelId: "", serialNumber: "", installedOn: "" }

/**
 * Novo equipamento (telas 17 e 18): o local vem da ficha, só leitura (com o endereço na web, só a
 * cidade no celular); salvar volta para a ficha. Sem a lista de modelos não há como salvar.
 */
function NewEquipment({
  customer,
  location,
}: {
  customer: CustomerDetail
  location: LocationView
}) {
  const isDesktop = useMediaQuery(MD_UP)
  const navigate = useNavigate()
  const install = useInstallEquipment()
  const options = useModelOptions()
  const model = useEquipmentForm(
    EMPTY,
    async (input) => {
      await install.mutateAsync({ locationId: location.id, input })
      void navigate(customerPath(customer.id))
    },
    TAKEN_HINT,
  )
  const { address } = location
  const place = isDesktop
    ? `${location.name} · ${address.street}, ${address.number} · ${address.city}/${address.state}`
    : `${location.name} · ${address.city}/${address.state}`

  return (
    <FormPage
      customer={customer}
      title="Novo equipamento"
      saveLabel="Salvar equipamento"
      model={model}
      alerts={
        options.error && (
          <Alert tone="danger">{errorMessage(options.error as Partial<ApiError>)}</Alert>
        )
      }
      saveDisabled={!options.data}
      head={
        <>
          <ReadOnly label="Local">{place}</ReadOnly>
          <ModelField model={model} options={options.data} />
        </>
      }
      serialHelp={SERIAL_HELP}
      serialHelpClass={WEB_HELP}
      // A tela 18 não desenha a nota no celular.
      tail={<Lock className="hidden md:flex">{NEW_LOCK}</Lock>}
    />
  )
}

/** O painel "Situação" ao lado, na web (D5, D6), como o do modelo do catálogo. */
function StatusAside({ children }: { children: React.ReactNode }) {
  return (
    <aside aria-label="Situação do equipamento" className={cn(PANEL, "w-85 shrink-0")}>
      <h2 className="text-label text-text-muted">Situação</h2>
      {children}
    </aside>
  )
}

const chip = (item: EquipmentView) => (
  <span className="self-start">
    <CustomerStatusChip status={item.status} />
  </span>
)

type Props = {
  customer: CustomerDetail
  location: LocationView
  equipment: EquipmentView
}

/**
 * Editar equipamento (tela 19 no celular, D5 na web): modelo e local só leitura (ADR-5), série e
 * data com os valores do próprio equipamento, e o `PUT` só com elas. "Desativar equipamento" sem
 * confirmação (ADR-6, ADR-32): a ficha se recarrega e a página passa ao desativado sem sair dela.
 */
function EditEquipment({ customer, location, equipment }: Props) {
  const isDesktop = useMediaQuery(MD_UP)
  const navigate = useNavigate()
  const update = useUpdateInstalledEquipment()
  const deactivate = useDeactivateInstalledEquipment()
  const model = useEquipmentForm(
    {
      modelId: equipment.model.id,
      serialNumber: equipment.serialNumber,
      installedOn: equipment.installedOn ?? "",
    },
    async ({ serialNumber, installedOn }) => {
      await update.mutateAsync({ id: equipment.id, input: { serialNumber, installedOn } })
      void navigate(customerPath(customer.id))
    },
  )
  const onDeactivate = () => {
    model.setFailure(null)
    deactivate.mutate(equipment.id, {
      onError: (e) => model.setFailure(e as Partial<ApiError>),
    })
  }

  return (
    <FormPage
      customer={customer}
      title={equipment.model.name}
      saveLabel="Salvar alterações"
      model={model}
      head={
        <>
          {!isDesktop && chip(equipment)}
          <ReadOnly label="Modelo">{equipmentText(equipment)}</ReadOnly>
          <ReadOnly label="Local" note={<Lock>{EDIT_LOCK}</Lock>}>
            {placeText(customer, location)}
          </ReadOnly>
        </>
      }
      serialHelp={FIX_SERIAL_HELP}
      serialHelpClass={HELP}
      // `.dng` da tela 19 (linha 37): 44 px, `danger-text`, 14 px 600, linha em cima, 6 de respiro.
      mobileFooter={
        <Button
          variant="danger-text"
          loading={deactivate.isPending}
          onClick={onDeactivate}
          className="w-full justify-start border-t border-line pt-1.5 text-body-sm"
        >
          Desativar equipamento
        </Button>
      }
      aside={
        <StatusAside>
          {chip(equipment)}
          <p className="text-body-sm text-text-muted">{DEACTIVATE_HELP}</p>
          {/* `.dng` do modelo (Modelo-web.dc.html, linha 44). */}
          <Button
            variant="danger-text"
            loading={deactivate.isPending}
            onClick={onDeactivate}
            className="min-h-11 self-start text-body-sm"
          >
            Desativar equipamento
          </Button>
        </StatusAside>
      }
    />
  )
}

/**
 * Equipamento desativado (tela 19 desligada, D6): só leitura, com a ação única "Reativar
 * equipamento"; reativar recarrega a ficha e a página volta ao formulário.
 */
function DeactivatedEquipment({ customer, location, equipment }: Props) {
  const isDesktop = useMediaQuery(MD_UP)
  const reactivate = useReactivateInstalledEquipment()
  const [failure, setFailure] = React.useState<Failure>(null)
  const fields: [string, React.ReactNode][] = [
    ["Modelo", equipmentText(equipment)],
    ["Local", placeText(customer, location)],
    ["Número de série", <span className="tabular-nums">{equipment.serialNumber}</span>],
    [
      "Instalado em",
      equipment.installedOn ? (
        <span className="font-display font-semibold tabular-nums">
          {formatDate(equipment.installedOn)}
        </span>
      ) : (
        <span className="text-text-muted">Não informado</span>
      ),
    ],
  ]

  const note = <p className="text-body-sm text-text-muted">{DEACTIVATED_NOTE}</p>
  const action = (
    <Button
      size={isDesktop ? "md" : "lg"}
      loading={reactivate.isPending}
      onClick={() => {
        setFailure(null)
        reactivate.mutate(equipment.id, {
          onError: (e) => setFailure(e as Partial<ApiError>),
        })
      }}
      className={isDesktop ? cn(WEB_BUTTON, "self-start") : "min-h-12 w-full"}
    >
      Reativar equipamento
    </Button>
  )
  // `.panel`, `.k`, `.v` e `.num` da tela 19 (linhas 32 a 35 e 85 a 90).
  const data = (
    <section aria-label="Dados do equipamento" className={cn(PANEL, "md:w-160")}>
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
  const title = equipment.model.name

  if (isDesktop)
    return (
      <Frame back={backTo(customer)} title={title}>
        {alert}
        <div className="flex items-start gap-6">
          {data}
          <StatusAside>
            {chip(equipment)}
            {note}
            {action}
          </StatusAside>
        </div>
      </Frame>
    )

  return (
    <Frame back={backTo(customer)} title={title} mobileAction={action}>
      {/* A tela 19 desligada tem 18 entre os blocos (linha 20). */}
      <div className="flex flex-col gap-4.5">
        {alert}
        {chip(equipment)}
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
 * Lê a ficha (sem `GET` do equipamento: ADR-22) e só monta a página com ela lida agora (a do cache
 * pode ser de antes da última escrita): até lá não há campo nem ação. Equipamento novo só com o
 * local e o cliente ativos (ADR-6); o equipamento de local ou de cliente desativado se edita, se
 * desativa e se reativa (ADR-30).
 */
function EquipmentLoader({
  mode,
  id,
  locationId,
  equipmentId,
}: {
  mode: "new" | "edit"
  id: string
  locationId: string
  equipmentId: string
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

  const notFound = (code: string) => (
    <Frame back={backTo(customer)}>
      <Alert tone="danger">{errorMessage({ code })}</Alert>
    </Frame>
  )

  if (mode === "new") {
    const location = customer.locations.find((item) => item.id === locationId)

    if (!location) return notFound("location_not_found")

    if (location.status !== "ACTIVE" || customer.status !== "ACTIVE")
      return <Navigate to={customerPath(id)} replace />

    return <NewEquipment customer={customer} location={location} />
  }

  const location = customer.locations.find((item) =>
    item.equipment.some((one) => one.id === equipmentId),
  )
  const equipment = location?.equipment.find((one) => one.id === equipmentId)

  if (!location || !equipment) return notFound("installed_equipment_not_found")

  if (equipment.status === "DEACTIVATED")
    return (
      <DeactivatedEquipment
        customer={customer}
        location={location}
        equipment={equipment}
      />
    )

  return (
    <EditEquipment
      key={equipment.id}
      customer={customer}
      location={location}
      equipment={equipment}
    />
  )
}

/**
 * `new` (telas 17 e 18) e `edit` (tela 19, D5, D6) do equipamento instalado. Sem `canEdit` (o
 * Técnico, ADR-7) as duas voltam para a ficha. Trocar de equipamento remonta tudo: a ficha é lida
 * de novo e o formulário não guarda o estado do anterior.
 */
function EquipmentFormScreen({
  mode,
  canEdit,
}: {
  mode: "new" | "edit"
  canEdit: boolean
}) {
  const { id = "", locationId = "", equipmentId = "" } = useParams()

  if (!canEdit) return <Navigate to={customerPath(id)} replace />

  return (
    <EquipmentLoader
      key={`${id}/${locationId}/${equipmentId}`}
      mode={mode}
      id={id}
      locationId={locationId}
      equipmentId={equipmentId}
    />
  )
}

export { EquipmentFormScreen }
