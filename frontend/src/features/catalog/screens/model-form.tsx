import { zodResolver } from "@hookform/resolvers/zod"
import { ChevronLeftIcon } from "lucide-react"
import * as React from "react"
import { useController, useForm, useWatch } from "react-hook-form"
import { Link, Navigate, useNavigate, useParams } from "react-router"

import type { ApiError } from "@/shared/api/api-error"
import { cn } from "@/shared/lib/cn"
import { MD_UP, useMediaQuery } from "@/shared/lib/use-media-query"
import { Alert } from "@/shared/ui/alert"
import { Button } from "@/shared/ui/button"
import { FieldError } from "@/shared/ui/field-error"
import { Input } from "@/shared/ui/input"
import { Label } from "@/shared/ui/label"
import { MobileHeader } from "@/shared/ui/mobile-header"
import { NativeSelect } from "@/shared/ui/native-select"
import { Spinner } from "@/shared/ui/spinner"

import { amountText, formatAmount, parseAmount } from "../amount"
import {
  useDeactivateEquipmentModel,
  useEquipmentModel,
  useEquipmentTypes,
  useManufacturers,
  useReactivateEquipmentModel,
  useRegisterEquipmentModel,
  useUpdateEquipmentModel,
} from "../catalog.hooks"
import { errorMessage, messageFor } from "../catalog.messages"
import {
  equipmentModelSchema,
  optionalAmountModelSchema,
  type EquipmentModelValues,
} from "../catalog.schemas"
import type {
  EquipmentModelSummary,
  EquipmentTypeSummary,
  ModelValuesInput,
} from "../catalog.service"
import { CatalogStatusChip } from "../components/catalog-status"
import { ModelValuesFields } from "../components/model-values-fields"

const LIST_PATH = "/catalogo"
const NAME_MAX = 80
// shortcut: as escolhas listam só os 100 primeiros; na edição isso só afeta a obrigatoriedade e o
// modelo sem valor salvo (Salvar fica desligado). Paginar a escolha se o catálogo passar disso.
const OPTIONS = { page: 0, size: 100 } as const

// Textos aprovados dos mockups 3, 5 e 6.
const MANUFACTURER_HELP = "Não achou o fabricante? Cadastre em Catálogo, Fabricantes."
const FIXED_HELP =
  "Fabricante e tipo não mudam depois de salvos: as características dependem do tipo."
const DEACTIVATE_HELP =
  "Desativar tira o modelo da lista de escolha para novas instalações. As unidades já instaladas continuam apontando para ele. Dá para reativar a qualquer momento."
const DEACTIVATED_TEXT =
  "Este modelo não aparece na lista de escolha para novas instalações. As unidades já instaladas continuam apontando para ele. Para editar, reative primeiro."

const FORM = "flex flex-col gap-4.5 md:gap-5 md:border md:border-line md:bg-panel md:p-6"
const PANEL = "flex flex-col gap-3 border border-line bg-panel p-4 md:gap-5 md:p-6"
const HELP = "text-caption text-text-muted"
// Botões da web (mockups 3 e 5): 44 px e 14 px peso 600; o do celular é o do rodapé fixo, 48 px e
// 16 px. `md:font-semibold` porque o `md:text-body-sm` traz o peso 400 do token.
const WEB_BUTTON = "md:text-body-sm md:font-semibold"

type Failure = Partial<ApiError> | null

/** As ligações de acessibilidade de um campo com erro (o texto do erro descreve o campo). */
const invalid = (id: string, error: string | undefined) => ({
  "aria-invalid": error ? true : undefined,
  "aria-describedby": error ? `${id}-error` : undefined,
})

/**
 * A moldura das telas do modelo. Na web: o link "Modelos" e o título no h1 (`display-lg`). No
 * celular: o cabeçalho próprio com "Voltar" (a casca não põe as barras em `/catalogo/modelos/`) e
 * a ação principal no rodapé fixo, com a folga dela no fim do conteúdo.
 */
function Frame({
  title,
  mobileAction,
  children,
}: {
  title?: string
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
            Modelos
          </Link>
          {title && <h1 className="text-display-lg">{title}</h1>}
        </div>
        {children}
      </main>
    )

  return (
    <>
      <MobileHeader to={LIST_PATH} title={title} />
      <main
        className={
          mobileAction
            ? "flex flex-col gap-4 px-4 pt-5 pb-24"
            : "flex flex-col gap-4 p-4 pt-5"
        }
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

type SendModel = (values: EquipmentModelValues, input: ModelValuesInput) => Promise<void>

/**
 * O formulário do modelo (novo e edição). O valor da característica é validado pelo tipo: vazio
 * só vale quando ela não é obrigatória. Os erros do servidor saem pelo `code`: o nome repetido
 * sob o nome (o texto digitado fica), o valor recusado sob o valor, o resto no aviso.
 */
function useModelForm(
  defaults: EquipmentModelValues,
  findType: (typeId: string) => EquipmentTypeSummary | undefined,
  send: SendModel,
  onStale?: () => void,
) {
  const [failure, setFailure] = React.useState<Failure>(null)
  const form = useForm<EquipmentModelValues>({
    defaultValues: defaults,
    resolver: (values, context, options) => {
      // Sem característica conhecida não há campo do valor, então não há valor a exigir.
      const optional = findType(values.typeId)?.characteristics[0]?.required !== true
      const schema = optional ? optionalAmountModelSchema : equipmentModelSchema

      return zodResolver(schema)(values, context, options)
    },
  })
  const type = findType(useWatch({ control: form.control, name: "typeId" }))
  const characteristic = type?.characteristics[0]
  const amount = useController({ control: form.control, name: "amount" })

  const submit = form.handleSubmit(async (values) => {
    setFailure(null)
    const parsed = parseAmount(values.amount)
    // A lista substitui a do modelo (usecases.md): vazia só quando a pessoa apagou o valor opcional.
    const input =
      characteristic && parsed.ok
        ? [{ definitionId: characteristic.id, amount: parsed.value }]
        : []

    try {
      await send(values, input)
    } catch (e) {
      const error = e as Partial<ApiError>

      if (error.code === "model_name_taken") {
        form.setError("name", { message: messageFor(error.code, "") })
        form.setFocus("name")
      } else if (characteristic && error.code === "characteristic_required") {
        // A tabela de erros traz o nome da característica, que a tela conhece pelo tipo.
        form.setError("amount", {
          message: `informe a característica ${characteristic.name}`,
        })
        form.setFocus("amount")
      } else if (characteristic && error.code === "invalid_amount") {
        form.setError("amount", { message: messageFor(error.code, "") })
        form.setFocus("amount")
      } else {
        // Sem o campo do valor na tela, o erro dele sai no aviso: nunca fica invisível.
        setFailure(error)
        if (error.code === "model_deactivated") onStale?.()
      }
    }
  })

  return { form, type, amount, failure, setFailure, submit }
}

type ModelForm = ReturnType<typeof useModelForm>

function NameField({ form }: { form: ModelForm["form"] }) {
  const id = React.useId()
  const error = form.formState.errors.name?.message

  return (
    <div className="flex flex-col gap-1.5">
      <Label htmlFor={id}>Nome do modelo *</Label>
      <Input
        id={id}
        size="compact"
        type="text"
        autoComplete="off"
        maxLength={NAME_MAX}
        {...invalid(id, error)}
        {...form.register("name")}
      />
      {error && <FieldError id={`${id}-error`}>{error}</FieldError>}
    </div>
  )
}

/** Os campos do formulário: o cabeçalho (fabricante e tipo), o nome e o valor da característica. */
function ModelFields({
  model,
  id,
  head,
  valueState,
  footer,
  className,
}: {
  model: ModelForm
  id: string
  head: React.ReactNode
  /** No lugar do valor, enquanto a característica não é conhecida (carregando ou erro). */
  valueState?: React.ReactNode
  footer?: React.ReactNode
  className?: string
}) {
  return (
    <form id={id} noValidate onSubmit={model.submit} className={cn(FORM, className)}>
      {model.failure && <Alert tone="danger">{errorMessage(model.failure)}</Alert>}
      {head}
      <NameField form={model.form} />
      <ModelValuesFields
        type={model.type}
        value={model.amount.field.value}
        onChange={model.amount.field.onChange}
        inputRef={model.amount.field.ref}
        error={model.form.formState.errors.amount?.message}
      />
      {valueState}
      {footer}
    </form>
  )
}

/** "Cancelar" e o botão de salvar, só na web (no celular o salvar é o rodapé fixo). */
function WebButtons({ children }: { children: React.ReactNode }) {
  return (
    <div className="flex justify-end gap-3">
      <Link
        to={LIST_PATH}
        className="cut-sm inline-flex min-h-11 items-center bg-chip-ok px-5 text-body-sm font-semibold text-text"
      >
        Cancelar
      </Link>
      {children}
    </div>
  )
}

function SaveButton({
  formId,
  label,
  model,
  disabled,
}: {
  formId: string
  label: string
  model: ModelForm
  disabled?: boolean
}) {
  const isDesktop = useMediaQuery(MD_UP)

  return (
    <Button
      type="submit"
      form={formId}
      size={isDesktop ? "md" : "lg"}
      disabled={disabled}
      loading={model.form.formState.isSubmitting}
      className={isDesktop ? WEB_BUTTON : "min-h-12 w-full"}
    >
      {label}
    </Button>
  )
}

/** Fabricante ou tipo depois de salvos (mockup 5): texto em caixa apagada, não campo. */
function Fixed({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex flex-col gap-1.5">
      <span className="text-label">{label}</span>
      <div className="flex h-12 items-center border border-line bg-nav px-3 text-body text-text-muted md:h-11">
        {value}
      </div>
    </div>
  )
}

/** Novo modelo (mockups 3 e 4): fabricante e tipo só entre os ativos. Salvar volta para a lista. */
function NewModel() {
  const isDesktop = useMediaQuery(MD_UP)
  const navigate = useNavigate()
  const formId = React.useId()
  const manufacturers = useManufacturers({ status: "ACTIVE", ...OPTIONS })
  const types = useEquipmentTypes({ status: "ACTIVE", ...OPTIONS })
  const register = useRegisterEquipmentModel()
  const model = useModelForm(
    { manufacturerId: "", typeId: "", name: "", amount: "" },
    (typeId) => types.data?.content.find((type) => type.id === typeId),
    async (values, input) => {
      await register.mutateAsync({
        manufacturerId: values.manufacturerId,
        typeId: values.typeId,
        name: values.name,
        values: input,
      })
      void navigate(LIST_PATH)
    },
  )
  const { errors } = model.form.formState
  const optionsFailure = (manufacturers.error ?? types.error) as Failure
  const save = <SaveButton formId={formId} label="Salvar modelo" model={model} />

  return (
    <Frame title="Novo modelo" mobileAction={!isDesktop && save}>
      <ModelFields
        model={model}
        id={formId}
        className="md:max-w-160"
        head={
          <>
            {optionsFailure ? (
              <Alert tone="danger">{errorMessage(optionsFailure)}</Alert>
            ) : (
              (manufacturers.isPending || types.isPending) && <Spinner />
            )}
            <div className="flex flex-col gap-1.5">
              <Label htmlFor={`${formId}-manufacturer`}>Fabricante *</Label>
              <NativeSelect
                id={`${formId}-manufacturer`}
                size="compact"
                {...invalid(`${formId}-manufacturer`, errors.manufacturerId?.message)}
                {...model.form.register("manufacturerId")}
              >
                <option value="" disabled>
                  Escolha o fabricante
                </option>
                {manufacturers.data?.content.map((manufacturer) => (
                  <option key={manufacturer.id} value={manufacturer.id}>
                    {manufacturer.name}
                  </option>
                ))}
              </NativeSelect>
              {errors.manufacturerId?.message && (
                <FieldError id={`${formId}-manufacturer-error`}>
                  {errors.manufacturerId.message}
                </FieldError>
              )}
              <p className={cn(HELP, "hidden md:block")}>{MANUFACTURER_HELP}</p>
            </div>
            <div className="flex flex-col gap-1.5">
              <Label htmlFor={`${formId}-type`}>Tipo *</Label>
              <NativeSelect
                id={`${formId}-type`}
                size="compact"
                {...invalid(`${formId}-type`, errors.typeId?.message)}
                {...model.form.register("typeId")}
              >
                <option value="" disabled>
                  Escolha o tipo
                </option>
                {types.data?.content.map((type) => (
                  <option key={type.id} value={type.id}>
                    {type.name}
                  </option>
                ))}
              </NativeSelect>
              {errors.typeId?.message && (
                <FieldError id={`${formId}-type-error`}>
                  {errors.typeId.message}
                </FieldError>
              )}
            </div>
          </>
        }
        footer={isDesktop && <WebButtons>{save}</WebButtons>}
      />
    </Frame>
  )
}

/**
 * Modelo ativo, para quem edita (mockup 5): fabricante e tipo só leitura (ADR-4), nome e valor
 * editáveis, e o painel de situação com "Desativar modelo", sem confirmação (ADR-5). O tipo vem
 * do `typeId` salvo e fica estável: a consulta que volta sem ele não apaga o que foi digitado.
 */
function EditModel({
  model: saved,
  onStale,
}: {
  model: EquipmentModelSummary
  onStale: () => void
}) {
  const isDesktop = useMediaQuery(MD_UP)
  const navigate = useNavigate()
  const formId = React.useId()
  const types = useEquipmentTypes({ status: "ALL", ...OPTIONS })
  const found = types.data?.content.find((type) => type.id === saved.type.id)
  const [lastType, setLastType] = React.useState(found)
  const update = useUpdateEquipmentModel()
  const deactivate = useDeactivateEquipmentModel()

  if (found && found !== lastType) setLastType(found)

  const value = saved.values[0]
  // A característica do valor salvo vem no próprio modelo; a lista de tipos só confirma se ela é
  // obrigatória (até chegar, vale como obrigatória). Sem valor salvo, só a lista a conhece.
  const type: EquipmentTypeSummary | undefined =
    found ??
    lastType ??
    (value && {
      ...saved.type,
      status: "ACTIVE",
      modelCount: 0,
      characteristics: [
        { id: value.definitionId, name: value.name, unit: value.unit, required: true },
      ],
    })
  const model = useModelForm(
    {
      manufacturerId: saved.manufacturer.id,
      typeId: saved.type.id,
      name: saved.name,
      amount: value ? formatAmount(value.amount) : "",
    },
    () => type,
    async (values, input) => {
      await update.mutateAsync({
        id: saved.id,
        input: { name: values.name, values: input },
      })
      void navigate(LIST_PATH)
    },
    onStale,
  )
  // Sem a característica não há como montar o valor: Salvar espera, em vez de apagar o salvo.
  const save = (
    <SaveButton
      formId={formId}
      label="Salvar alterações"
      model={model}
      disabled={!type}
    />
  )
  const typesFailure = types.error as Failure
  const valueState =
    !type &&
    (typesFailure ? (
      <Alert tone="danger">{errorMessage(typesFailure)}</Alert>
    ) : (
      types.isPending && <Spinner />
    ))

  return (
    <Frame title={saved.name} mobileAction={!isDesktop && save}>
      <div className="flex flex-col gap-4 md:flex-row md:items-start md:gap-6">
        <ModelFields
          model={model}
          id={formId}
          className="md:w-160"
          head={
            <>
              <Fixed label="Fabricante" value={saved.manufacturer.name} />
              <div className="flex flex-col gap-1.5">
                <Fixed label="Tipo" value={saved.type.name} />
                <p className={HELP}>{FIXED_HELP}</p>
              </div>
            </>
          }
          valueState={valueState}
          footer={isDesktop && <WebButtons>{save}</WebButtons>}
        />
        <aside aria-label="Situação do modelo" className={cn(PANEL, "md:w-85")}>
          <h2 className="text-label text-text-muted">Situação</h2>
          <span className="self-start">
            <CatalogStatusChip status={saved.status} />
          </span>
          <p className="text-body-sm text-text-muted">{DEACTIVATE_HELP}</p>
          <Button
            variant="danger-text"
            loading={deactivate.isPending}
            onClick={() => {
              model.setFailure(null)
              deactivate.mutate(saved.id, {
                onError: (e) => model.setFailure(e as Partial<ApiError>),
              })
            }}
            className="min-h-11 self-start text-body-sm"
          >
            Desativar modelo
          </Button>
        </aside>
      </div>
    </Frame>
  )
}

/**
 * Modelo só leitura (mockup 6): desativado, ou aberto por quem não edita (ADR-6). Os dados em
 * texto e, para quem edita o desativado, a ação única "Reativar modelo". Na web (derivada da 5),
 * os dados à esquerda e a situação no painel ao lado.
 */
function ReadOnlyModel({
  model,
  canEdit,
}: {
  model: EquipmentModelSummary
  canEdit: boolean
}) {
  const isDesktop = useMediaQuery(MD_UP)
  const reactivate = useReactivateEquipmentModel()
  const [failure, setFailure] = React.useState<Failure>(null)
  const deactivated = model.status === "DEACTIVATED"
  const value = model.values[0]

  const status = (
    <span className="self-start">
      <CatalogStatusChip status={model.status} />
    </span>
  )
  const note = deactivated && (
    <p className="text-body-sm text-text-muted">{DEACTIVATED_TEXT}</p>
  )
  const action = canEdit && deactivated && (
    <Button
      size={isDesktop ? "md" : "lg"}
      loading={reactivate.isPending}
      onClick={() => {
        setFailure(null)
        reactivate.mutate(model.id, {
          onError: (e) => setFailure(e as Partial<ApiError>),
        })
      }}
      className={isDesktop ? cn(WEB_BUTTON, "self-start") : "min-h-12 w-full"}
    >
      Reativar modelo
    </Button>
  )
  const data = (
    <section aria-label="Dados do modelo" className={cn(PANEL, "gap-3.5 md:w-160")}>
      <dl className="flex flex-col gap-3.5 md:gap-5">
        {[
          ["Fabricante", model.manufacturer.name],
          ["Tipo", model.type.name],
        ].map(([key, text]) => (
          <div key={key}>
            <dt className="text-label text-text-muted">{key}</dt>
            <dd className="text-body">{text}</dd>
          </div>
        ))}
        {value && (
          <div>
            <dt className="text-label text-text-muted">{value.name}</dt>
            <dd className="text-number tabular-nums">{amountText(value)}</dd>
          </div>
        )}
      </dl>
    </section>
  )
  const alert = failure && <Alert tone="danger">{errorMessage(failure)}</Alert>

  if (isDesktop)
    return (
      <Frame title={model.name}>
        {alert}
        <div className="flex items-start gap-6">
          {data}
          <aside aria-label="Situação do modelo" className={cn(PANEL, "w-85")}>
            <h2 className="text-label text-text-muted">Situação</h2>
            {status}
            {note}
            {action}
          </aside>
        </div>
      </Frame>
    )

  return (
    <Frame title={model.name} mobileAction={action}>
      {alert}
      {status}
      {data}
      {note}
    </Frame>
  )
}

function ModelDetail({ canEdit }: { canEdit: boolean }) {
  const { id = "" } = useParams()
  const query = useEquipmentModel(id)
  const model = query.data
  const failure = query.error as Failure

  if (!model)
    return (
      <Frame>
        {failure ? <Alert tone="danger">{errorMessage(failure)}</Alert> : <Spinner />}
      </Frame>
    )

  if (!canEdit || model.status === "DEACTIVATED")
    return <ReadOnlyModel model={model} canEdit={canEdit} />

  return <EditModel key={model.id} model={model} onStale={() => void query.refetch()} />
}

/**
 * `new` (mockups 3 e 4) e `edit` (mockups 5 e 6) do modelo. Sem `canEdit` (o Técnico, ADR-6) o
 * novo volta para a lista e o modelo abre só leitura, sem nenhum botão de escrita.
 */
function ModelFormScreen({ mode, canEdit }: { mode: "new" | "edit"; canEdit: boolean }) {
  if (mode === "edit") return <ModelDetail canEdit={canEdit} />
  if (!canEdit) return <Navigate to={LIST_PATH} replace />

  return <NewModel />
}

export { ModelFormScreen }
