import { zodResolver } from "@hookform/resolvers/zod"
import { LockIcon } from "lucide-react"
import * as React from "react"
import { useForm } from "react-hook-form"
import { Navigate, useNavigate, useParams } from "react-router"

import type { ApiError } from "@/shared/api/api-error"
import { cn } from "@/shared/lib/cn"
import { MD_UP, useMediaQuery } from "@/shared/lib/use-media-query"
import { Alert } from "@/shared/ui/alert"
import { Button } from "@/shared/ui/button"
import { Checkbox } from "@/shared/ui/checkbox"
import { Input } from "@/shared/ui/input"
import { Label } from "@/shared/ui/label"
import { NativeSelect } from "@/shared/ui/native-select"

import {
  useDeactivateEquipmentType,
  useEquipmentTypes,
  useReactivateEquipmentType,
  useRegisterEquipmentType,
  useUpdateEquipmentType,
} from "../catalog.hooks"
import { errorMessage, LABELS, messageFor } from "../catalog.messages"
import { equipmentTypeSchema, type EquipmentTypeValues } from "../catalog.schemas"
import type { EquipmentTypeInput, EquipmentTypeSummary } from "../catalog.service"
import { CatalogStatusChip } from "../components/catalog-status"
import { FieldError } from "../components/model-values-fields"
import { PanelFrame, SaveButton, Spinner, WEB_BUTTON } from "../components/panel-frame"

const LIST_PATH = "/catalogo/tipos"
const NAME_MAX = 60
const CHARACTERISTIC_MAX = 40
// shortcut: não há consulta de um tipo só nem paginação no mockup 9; a lista traz os 100 primeiros,
// de todas as situações (o desativado precisa ficar ao alcance de Reativar). Paginar se a empresa
// passar disso.
const TYPE_LIST = { status: "ALL", page: 0, size: 100 } as const

// Textos aprovados dos mockups 9 e 11.
const NAME_HELP = "Exemplos: Inversor, Ar-condicionado."
const UNIT_HELP = "Depois que o tipo tiver modelos, a unidade não muda."
const LOCK_NOTICE = "A unidade não muda porque o tipo já tem modelos cadastrados."
const ONE_HELP = "Nesta versão cada tipo tem uma característica."
const DEACTIVATED_TEXT =
  "Este tipo não aparece na escolha para modelos novos. Para editar, reative primeiro."
const HELP = "text-caption text-text-muted"

type Failure = Partial<ApiError> | null

// O mockup 11 vem com a caixa marcada; a unidade não vem escolhida (o schema obriga a escolher).
const BLANK: EquipmentTypeValues = {
  name: "",
  characteristicName: "",
  unit: "" as EquipmentTypeValues["unit"],
  required: true,
}

const toInput = (values: EquipmentTypeValues): EquipmentTypeInput => ({
  name: values.name,
  characteristic: {
    name: values.characteristicName,
    unit: values.unit,
    required: values.required,
  },
})

/** Os valores de um tipo salvo; um tipo sem característica (o servidor não devolve) abre vazio. */
function valuesOf(type: EquipmentTypeSummary): EquipmentTypeValues {
  const characteristic = type.characteristics[0]

  return characteristic
    ? {
        name: type.name,
        characteristicName: characteristic.name,
        unit: characteristic.unit,
        required: characteristic.required,
      }
    : { ...BLANK, name: type.name }
}

/**
 * O formulário do tipo (novo e edição). Os erros do servidor saem pelo `code`: o nome repetido sob
 * o campo (o texto digitado fica), o resto no aviso. `onStale` refaz a consulta quando o servidor
 * diz que o tipo já está desativado, para a tela mostrar a situação de verdade.
 */
function useTypeForm(
  defaults: EquipmentTypeValues,
  send: (input: EquipmentTypeInput) => Promise<void>,
  onStale?: () => void,
) {
  const [failure, setFailure] = React.useState<Failure>(null)
  const form = useForm<EquipmentTypeValues>({
    defaultValues: defaults,
    resolver: zodResolver(equipmentTypeSchema),
  })

  const submit = form.handleSubmit(async (values) => {
    setFailure(null)

    try {
      await send(toInput(values))
      // Na web o painel da própria lista não remonta ao voltar para a mesma URL: o formulário esvazia aqui.
      form.reset()
    } catch (e) {
      const error = e as Partial<ApiError>

      if (error.code === "type_name_taken") {
        form.setError("name", { message: messageFor(error.code, "") })
        form.setFocus("name")
      } else {
        setFailure(error)
        if (error.code === "characteristic_locked") {
          // A contagem de modelos estava velha: volta ao valor salvo e a consulta refeita trava os campos.
          form.resetField("unit")
          form.resetField("required")
          onStale?.()
        }
        if (error.code === "type_deactivated") onStale?.()
      }
    }
  })

  return { form, failure, setFailure, submit }
}

type TypeForm = ReturnType<typeof useTypeForm>

/** As ligações de acessibilidade de um campo com erro (o texto do erro descreve o campo). */
const invalid = (id: string, error: string | undefined, fallback?: string) => ({
  "aria-invalid": error ? true : undefined,
  "aria-describedby": error ? `${id}-error` : fallback,
})

/**
 * Os campos (mockups 9 e 11). `disabled` é o tipo desativado (tudo só leitura); `locked` é o tipo
 * com modelos: a unidade e a caixa "Obrigatória" ficam desabilitadas, com o aviso do mockup 9 (a
 * caixa é a ampliação do ADR-4: o backend só recusa ligá-la).
 */
function TypeFields({
  form,
  disabled,
  locked,
  isNew,
}: {
  form: TypeForm["form"]
  disabled: boolean
  locked: boolean
  isNew: boolean
}) {
  const id = React.useId()
  const { errors } = form.formState
  const unitHelp = locked ? `${id}-lock` : `${id}-unit-help`

  return (
    <>
      <div className="flex flex-col gap-1.5">
        <Label htmlFor={`${id}-name`}>Nome do tipo *</Label>
        <Input
          id={`${id}-name`}
          size="compact"
          type="text"
          autoComplete="off"
          maxLength={NAME_MAX}
          disabled={disabled}
          {...invalid(
            `${id}-name`,
            errors.name?.message,
            isNew ? `${id}-name-help` : undefined,
          )}
          {...form.register("name")}
        />
        {errors.name?.message && (
          <FieldError id={`${id}-name-error`}>{errors.name.message}</FieldError>
        )}
        {isNew && (
          <p id={`${id}-name-help`} className={HELP}>
            {NAME_HELP}
          </p>
        )}
      </div>

      <section
        aria-labelledby={`${id}-characteristic`}
        className="flex flex-col gap-3.5 border-t border-line pt-4.5 md:gap-3 md:pt-4"
      >
        <h3 id={`${id}-characteristic`} className="text-label text-text-muted">
          Característica
        </h3>
        <div className="flex flex-col gap-1.5">
          <Label htmlFor={`${id}-cname`}>Nome da característica *</Label>
          <Input
            id={`${id}-cname`}
            size="compact"
            type="text"
            autoComplete="off"
            maxLength={CHARACTERISTIC_MAX}
            disabled={disabled}
            {...invalid(`${id}-cname`, errors.characteristicName?.message)}
            {...form.register("characteristicName")}
          />
          {errors.characteristicName?.message && (
            <FieldError id={`${id}-cname-error`}>
              {errors.characteristicName.message}
            </FieldError>
          )}
        </div>
        <div className="flex flex-col gap-1.5">
          <Label htmlFor={`${id}-unit`}>Unidade *</Label>
          <NativeSelect
            id={`${id}-unit`}
            size="compact"
            disabled={disabled || locked}
            {...invalid(`${id}-unit`, errors.unit?.message, unitHelp)}
            {...form.register("unit")}
          >
            <option value="" disabled>
              Escolha a unidade
            </option>
            {Object.entries(LABELS.unit).map(([unit, label]) => (
              <option key={unit} value={unit}>
                {label}
              </option>
            ))}
          </NativeSelect>
          {errors.unit?.message && (
            <FieldError id={`${id}-unit-error`}>{errors.unit.message}</FieldError>
          )}
          {locked ? (
            <p id={`${id}-lock`} className={cn(HELP, "flex items-start gap-1.5")}>
              <LockIcon
                aria-hidden="true"
                strokeWidth={2.5}
                className="size-3.5 shrink-0"
              />
              {LOCK_NOTICE}
            </p>
          ) : (
            <p id={`${id}-unit-help`} className={HELP}>
              {UNIT_HELP}
            </p>
          )}
        </div>
        <Checkbox
          label="Obrigatória no cadastro do modelo"
          disabled={disabled || locked}
          {...form.register("required")}
        />
        <p className={cn(HELP, "hidden md:block")}>{ONE_HELP}</p>
      </section>
    </>
  )
}

/** Novo tipo (mockups 9 e 11, o painel da web derivado do 9 vazio): Salvar volta para a lista. */
function NewType() {
  const isDesktop = useMediaQuery(MD_UP)
  const navigate = useNavigate()
  const formId = React.useId()
  const register = useRegisterEquipmentType()
  const model = useTypeForm(BLANK, async (input) => {
    await register.mutateAsync(input)
    void navigate(LIST_PATH)
  })
  const save = (
    <SaveButton
      formId={formId}
      label="Salvar tipo"
      loading={model.form.formState.isSubmitting}
    />
  )

  return (
    <PanelFrame to={LIST_PATH} title="Novo tipo" mobileAction={!isDesktop && save}>
      {model.failure && <Alert tone="danger">{errorMessage(model.failure)}</Alert>}
      <form
        id={formId}
        noValidate
        onSubmit={model.submit}
        className="flex flex-col gap-4.5 md:gap-4"
      >
        <TypeFields form={model.form} disabled={false} locked={false} isNew />
        {isDesktop && save}
      </form>
    </PanelFrame>
  )
}

/**
 * Tipo da lista (mockup 9; no celular, o mockup 11 com os dados e o aviso, derivado). Ativo:
 * editar e desativar, sem confirmação (ADR-5); com modelos, a unidade e a caixa travam. Desativado:
 * campos só leitura e a ação única "Reativar tipo". Desativar e reativar ficam na tela; salvar
 * volta para a lista.
 */
function EditType({
  saved,
  onStale,
}: {
  saved: EquipmentTypeSummary
  onStale: () => void
}) {
  const isDesktop = useMediaQuery(MD_UP)
  const navigate = useNavigate()
  const formId = React.useId()
  const update = useUpdateEquipmentType()
  const deactivate = useDeactivateEquipmentType()
  const reactivate = useReactivateEquipmentType()
  const deactivated = saved.status === "DEACTIVATED"
  const model = useTypeForm(
    valuesOf(saved),
    async (input) => {
      await update.mutateAsync({ id: saved.id, input })
      void navigate(LIST_PATH)
    },
    onStale,
  )
  const toggle = (action: typeof deactivate) => {
    model.setFailure(null)
    action.mutate(saved.id, { onError: (e) => model.setFailure(e as Partial<ApiError>) })
  }
  const fields = (
    <TypeFields
      form={model.form}
      disabled={deactivated}
      locked={saved.modelCount > 0}
      isNew={false}
    />
  )
  const alert = model.failure && (
    <Alert tone="danger">{errorMessage(model.failure)}</Alert>
  )
  const title = isDesktop ? `Editar tipo: ${saved.name}` : "Editar tipo"

  if (deactivated) {
    const action = (
      <Button
        size={isDesktop ? "md" : "lg"}
        loading={reactivate.isPending}
        onClick={() => toggle(reactivate)}
        className={isDesktop ? WEB_BUTTON : "min-h-12 w-full"}
      >
        Reativar tipo
      </Button>
    )

    return (
      <PanelFrame to={LIST_PATH} title={title} mobileAction={!isDesktop && action}>
        {alert}
        {fields}
        <span className="self-start">
          <CatalogStatusChip status={saved.status} />
        </span>
        <p className="text-body-sm text-text-muted">{DEACTIVATED_TEXT}</p>
        {isDesktop && action}
      </PanelFrame>
    )
  }

  const save = (
    <SaveButton
      formId={formId}
      label="Salvar alterações"
      loading={model.form.formState.isSubmitting}
    />
  )

  return (
    <PanelFrame to={LIST_PATH} title={title} mobileAction={!isDesktop && save}>
      {alert}
      <form
        id={formId}
        noValidate
        onSubmit={model.submit}
        className="flex flex-col gap-4.5 md:gap-4"
      >
        {fields}
        {isDesktop && save}
      </form>
      <Button
        variant="danger-text"
        loading={deactivate.isPending}
        onClick={() => toggle(deactivate)}
        className="min-h-11 self-start text-body-sm"
      >
        Desativar tipo
      </Button>
    </PanelFrame>
  )
}

/** Não há consulta de um tipo só: ele vem da mesma lista da tela (mesma chave, sem pedido a mais). */
function TypeDetail() {
  const { id = "" } = useParams()
  const list = useEquipmentTypes(TYPE_LIST)
  const saved = list.data?.content.find((type) => type.id === id)
  const failure = list.error as Failure

  if (!saved)
    return (
      <PanelFrame to={LIST_PATH} title="Editar tipo">
        {failure ? (
          <Alert tone="danger">{errorMessage(failure)}</Alert>
        ) : list.isPending ? (
          <Spinner />
        ) : (
          <Alert tone="danger">{errorMessage({ code: "type_not_found" })}</Alert>
        )}
      </PanelFrame>
    )

  // A chave inclui a situação: ao desativar ou reativar o formulário nasce de novo, com o tipo salvo.
  return (
    <EditType
      key={`${saved.id}:${saved.status}`}
      saved={saved}
      onStale={() => void list.refetch()}
    />
  )
}

/**
 * `new` e `edit` do tipo (mockups 9 e 11; no celular, telas cheias). Sem `canEdit` (o Técnico,
 * ADR-6) não há painel nem botão de escrita: a rota volta para a lista.
 */
function TypePanel({ mode, canEdit }: { mode: "new" | "edit"; canEdit: boolean }) {
  if (!canEdit) return <Navigate to={LIST_PATH} replace />

  return mode === "new" ? <NewType /> : <TypeDetail />
}

export { TYPE_LIST, TypePanel }
