import { zodResolver } from "@hookform/resolvers/zod"
import * as React from "react"
import { useForm } from "react-hook-form"
import { Link, Navigate, useNavigate, useParams } from "react-router"

import type { ApiError } from "@/shared/api/api-error"
import { MD_UP, useMediaQuery } from "@/shared/lib/use-media-query"
import { Alert } from "@/shared/ui/alert"
import { Button } from "@/shared/ui/button"
import { FieldError } from "@/shared/ui/field-error"
import { Input } from "@/shared/ui/input"
import { Label } from "@/shared/ui/label"
import { Spinner } from "@/shared/ui/spinner"

import {
  useDeactivateManufacturer,
  useManufacturers,
  useReactivateManufacturer,
  useRegisterManufacturer,
  useRenameManufacturer,
} from "../catalog.hooks"
import { errorMessage, messageFor } from "../catalog.messages"
import { manufacturerSchema, type ManufacturerValues } from "../catalog.schemas"
import type { ManufacturerSummary } from "../catalog.service"
import { CatalogStatusChip } from "../components/catalog-status"
import { PanelFrame, SaveButton, WEB_BUTTON } from "../components/panel-frame"

const LIST_PATH = "/catalogo/fabricantes"
const NAME_MAX = 80
// shortcut: não há consulta de um fabricante só nem paginação no mockup 7; a lista traz os 100
// primeiros, de todas as situações (o desativado precisa ficar ao alcance de Reativar). Paginar se
// a empresa passar disso.
const MANUFACTURER_LIST = { status: "ALL", page: 0, size: 100 } as const

// Texto aprovado do mockup 7 (legenda e ajuda do painel).
const NAME_HELP = "Só o nome. Mais dados entram quando a equipe pedir."
const DEACTIVATE_HELP =
  "Desativar tira o fabricante da lista de escolha ao cadastrar modelo; os modelos dele continuam no catálogo. Dá para reativar a qualquer momento."
const DEACTIVATED_TEXT =
  "Este fabricante não aparece na lista de escolha ao cadastrar modelo; os modelos dele continuam no catálogo. Para renomear, reative primeiro."

type Failure = Partial<ApiError> | null

/**
 * O formulário do nome (novo e edição). Os erros do servidor saem pelo `code`: o nome repetido sob
 * o campo (o texto digitado fica), o resto no aviso. `onStale` refaz a consulta quando o servidor
 * diz que o fabricante já está desativado, para a tela mostrar a situação de verdade.
 */
function useNameForm(
  name: string,
  send: (name: string) => Promise<void>,
  onStale?: () => void,
) {
  const [failure, setFailure] = React.useState<Failure>(null)
  const form = useForm<ManufacturerValues>({
    defaultValues: { name },
    resolver: zodResolver(manufacturerSchema),
  })

  const submit = form.handleSubmit(async (values) => {
    setFailure(null)

    try {
      await send(values.name)
      // Na web o painel da própria lista não remonta ao voltar para a mesma URL: o campo esvazia aqui.
      form.reset()
    } catch (e) {
      const error = e as Partial<ApiError>

      if (error.code === "manufacturer_name_taken") {
        form.setError("name", { message: messageFor(error.code, "") })
        form.setFocus("name")
      } else {
        setFailure(error)
        if (error.code === "manufacturer_deactivated") onStale?.()
      }
    }
  })

  return { form, failure, setFailure, submit }
}

type NameForm = ReturnType<typeof useNameForm>

function NameField({
  form,
  placeholder,
}: {
  form: NameForm["form"]
  placeholder?: string
}) {
  const id = React.useId()
  const error = form.formState.errors.name?.message

  return (
    <div className="flex flex-col gap-1.5">
      <Label htmlFor={id}>Nome *</Label>
      <Input
        id={id}
        size="compact"
        type="text"
        autoComplete="off"
        maxLength={NAME_MAX}
        placeholder={placeholder}
        aria-invalid={error ? true : undefined}
        aria-describedby={error ? `${id}-error` : `${id}-help`}
        {...form.register("name")}
      />
      {error && <FieldError id={`${id}-error`}>{error}</FieldError>}
      <p id={`${id}-help`} className="text-caption text-text-muted">
        {NAME_HELP}
      </p>
    </div>
  )
}

/** Novo fabricante (mockups 7 e 8, o do celular derivado do 11): só o nome. Salvar volta para a lista. */
function NewManufacturer() {
  const isDesktop = useMediaQuery(MD_UP)
  const navigate = useNavigate()
  const formId = React.useId()
  const register = useRegisterManufacturer()
  const model = useNameForm("", async (name) => {
    await register.mutateAsync({ name })
    void navigate(LIST_PATH)
  })
  const save = (
    <SaveButton
      formId={formId}
      label="Salvar fabricante"
      loading={model.form.formState.isSubmitting}
    />
  )

  return (
    <PanelFrame to={LIST_PATH} title="Novo fabricante" mobileAction={!isDesktop && save}>
      {model.failure && <Alert tone="danger">{errorMessage(model.failure)}</Alert>}
      <form
        id={formId}
        noValidate
        onSubmit={model.submit}
        className="flex flex-col gap-4 md:gap-4"
      >
        <NameField form={model.form} placeholder="Ex.: nome do fabricante" />
        {isDesktop && save}
      </form>
    </PanelFrame>
  )
}

/**
 * Fabricante da lista (derivado do mockup 7: "Editar abre o mesmo painel, com o nome preenchido e
 * a ação Desativar fabricante"). Ativo: renomear e desativar, sem confirmação (ADR-5). Desativado:
 * nome só leitura (o domínio recusa renomear) e a ação única "Reativar fabricante". Desativar e
 * reativar ficam na tela; salvar o nome volta para a lista.
 */
function EditManufacturer({
  saved,
  onStale,
}: {
  saved: ManufacturerSummary
  onStale: () => void
}) {
  const isDesktop = useMediaQuery(MD_UP)
  const navigate = useNavigate()
  const formId = React.useId()
  const rename = useRenameManufacturer()
  const deactivate = useDeactivateManufacturer()
  const reactivate = useReactivateManufacturer()
  const deactivated = saved.status === "DEACTIVATED"
  const model = useNameForm(
    saved.name,
    async (name) => {
      await rename.mutateAsync({ id: saved.id, input: { name } })
      void navigate(LIST_PATH)
    },
    onStale,
  )
  const toggle = (action: typeof deactivate) => {
    model.setFailure(null)
    action.mutate(saved.id, { onError: (e) => model.setFailure(e as Partial<ApiError>) })
  }
  const alert = model.failure && (
    <Alert tone="danger">{errorMessage(model.failure)}</Alert>
  )

  if (deactivated) {
    const action = (
      <Button
        size={isDesktop ? "md" : "lg"}
        loading={reactivate.isPending}
        onClick={() => toggle(reactivate)}
        className={isDesktop ? WEB_BUTTON : "min-h-12 w-full"}
      >
        Reativar fabricante
      </Button>
    )
    const nameId = `${formId}-name`

    return (
      <PanelFrame
        to={LIST_PATH}
        title="Editar fabricante"
        mobileAction={!isDesktop && action}
      >
        {alert}
        <div className="flex flex-col gap-1.5">
          <Label htmlFor={nameId}>Nome *</Label>
          <Input id={nameId} size="compact" disabled value={saved.name} readOnly />
        </div>
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
    <PanelFrame
      to={LIST_PATH}
      title="Editar fabricante"
      mobileAction={!isDesktop && save}
    >
      {alert}
      <form
        id={formId}
        noValidate
        onSubmit={model.submit}
        className="flex flex-col gap-4"
      >
        <NameField form={model.form} />
        {isDesktop && (
          <div className="flex gap-3">
            {save}
            <Link
              to={LIST_PATH}
              className="cut-sm inline-flex min-h-11 items-center bg-chip-ok px-5 text-body-sm font-semibold text-text"
            >
              Cancelar
            </Link>
          </div>
        )}
      </form>
      <section
        aria-label="Situação do fabricante"
        className="flex flex-col gap-3 border-t border-line pt-4"
      >
        <h3 className="text-label text-text-muted">Situação</h3>
        <span className="self-start">
          <CatalogStatusChip status={saved.status} />
        </span>
        <p className="text-body-sm text-text-muted">{DEACTIVATE_HELP}</p>
        <Button
          variant="danger-text"
          loading={deactivate.isPending}
          onClick={() => toggle(deactivate)}
          className="min-h-11 self-start text-body-sm"
        >
          Desativar fabricante
        </Button>
      </section>
    </PanelFrame>
  )
}

/** Não há consulta de um fabricante só: ele vem da mesma lista da tela (mesma chave, sem pedido a mais). */
function ManufacturerDetail() {
  const { id = "" } = useParams()
  const list = useManufacturers(MANUFACTURER_LIST)
  const saved = list.data?.content.find((manufacturer) => manufacturer.id === id)
  const failure = list.error as Failure

  if (!saved)
    return (
      <PanelFrame to={LIST_PATH} title="Editar fabricante">
        {failure ? (
          <Alert tone="danger">{errorMessage(failure)}</Alert>
        ) : list.isPending ? (
          <Spinner />
        ) : (
          <Alert tone="danger">{errorMessage({ code: "manufacturer_not_found" })}</Alert>
        )}
      </PanelFrame>
    )

  // A chave inclui a situação: ao desativar ou reativar o formulário nasce de novo, com o nome salvo.
  return (
    <EditManufacturer
      key={`${saved.id}:${saved.status}`}
      saved={saved}
      onStale={() => void list.refetch()}
    />
  )
}

/**
 * `new` e `edit` do fabricante (mockup 7; no celular, telas cheias derivadas do mockup 11). Sem
 * `canEdit` (o Técnico, ADR-6) não há painel nem botão de escrita: a rota volta para a lista.
 */
function ManufacturerPanel({
  mode,
  canEdit,
}: {
  mode: "new" | "edit"
  canEdit: boolean
}) {
  if (!canEdit) return <Navigate to={LIST_PATH} replace />

  return mode === "new" ? <NewManufacturer /> : <ManufacturerDetail />
}

export { MANUFACTURER_LIST, ManufacturerPanel }
