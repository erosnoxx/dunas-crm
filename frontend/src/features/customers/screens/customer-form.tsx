import { zodResolver } from "@hookform/resolvers/zod"
import { ChevronRightIcon, LockIcon } from "lucide-react"
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
import { Spinner } from "@/shared/ui/spinner"

import { type Back, type Failure, Frame, WEB_BUTTON } from "../components/form-frame"
import { useCustomer, useRegisterCustomer, useUpdateCustomer } from "../customers.hooks"
import { errorMessage } from "../customers.messages"
import { customerSchema, type CustomerValues } from "../customers.schemas"
import type { CustomerDetail, CustomerInput } from "../customers.service"
import { describedBy } from "../display"
import { documentTypeOf, formatDocument, normalizeDocument } from "../document"
import { formatPhone, isBrazilian, maskPhone, toE164 } from "../phone"

const LIST_PATH = "/clientes"
const customerPath = (id: string) => `${LIST_PATH}/${id}`

// Limites do domínio: `CustomerName` (2 a 150) e o e-mail (254), que não tem mensagem (usecases.md).
const NAME_MAX = 150
const EMAIL_MAX = 254

// Textos aprovados das telas 7 e 8; a dica do documento e a do telefone estrangeiro são as da
// Tarefa 1 (design/README.md, "Decisões da implementação").
const DOCUMENT_HELP =
  "Digite o CPF ou o CNPJ. O tipo (CPF ou CNPJ) e a pontuação aparecem ao digitar; os dígitos verificadores são conferidos antes de salvar."
const NEW_LOCK =
  "Confira antes de salvar: o documento não pode ser alterado depois de salvo."
const EDIT_LOCK = "O documento não pode ser alterado depois de salvo."
const TRADE_NAME_HELP = "Só para CNPJ. A busca da lista acha o cliente também por ele."
const CONTACT_TITLE = "Contato: informe pelo menos um, e-mail ou telefone"
const PHONE_HELP = "Fixo ou celular, com DDD."
const FOREIGN_PHONE_HELP = "Número de fora do Brasil: não se edita nesta tela"
const TAKEN = "customer_document_taken"

// Painel da tela 7 (Cliente-formulario-web.dc.html:28): `panel`, contorno `line`, 24 px de
// respiro, 20 entre os blocos, até 640 px; no celular (Novo-cliente-celular-erros.dc.html:20),
// 18 entre os blocos, sem painel.
const FORM =
  "flex flex-col gap-4.5 md:max-w-160 md:gap-5 md:border md:border-line md:bg-panel md:p-6"
const FIELD = "flex flex-col gap-1.5"
// `.help` e `.lock` (tela 7, linhas 33 e 34): 12/16 `text-muted`, no peso do corpo.
const HELP = "text-caption font-normal text-text-muted"
// `contact` não é campo: é o caminho do erro "pelo menos um contato" (customers.schemas.ts), que
// sai no topo da seção Contato.
type FormValues = CustomerValues & { contact?: string }

const isCompany = (document: string) => documentTypeOf(document) === "CNPJ"
const resolveCustomer = zodResolver(customerSchema)

type Send = (input: CustomerInput, values: CustomerValues) => Promise<void>

/**
 * O formulário do cliente (novo e edição). O nome fantasia só vale com CNPJ: escondido, ele não é
 * conferido e vai como `null`. Opcional vazio vai como `null` e o telefone em E.164 (ADR-18, 31);
 * o estrangeiro guardado volta como está. Os erros do servidor saem pelo `code` no campo
 * (usecases.md, "Onde aparece"); o resto, no aviso.
 */
function useCustomerForm(defaults: CustomerValues, send: Send, foreignPhone?: string) {
  const [failure, setFailure] = React.useState<Failure>(null)
  const form = useForm<FormValues>({
    defaultValues: defaults,
    resolver: (values, context, options) =>
      resolveCustomer(
        isCompany(values.document) ? values : { ...values, tradeName: "" },
        context,
        options,
      ),
  })
  const company = isCompany(useWatch({ control: form.control, name: "document" }))

  const submit = form.handleSubmit(
    async (values) => {
      setFailure(null)

      try {
        await send(
          {
            name: values.name,
            tradeName: values.tradeName || null,
            email: values.email || null,
            phone: foreignPhone ?? toE164(values.phone),
          },
          values,
        )
      } catch (e) {
        const error = e as Partial<ApiError>
        const message = errorMessage(error)

        if (error.code === TAKEN) {
          form.setError("document", { type: TAKEN, message })
          form.setFocus("document")
        } else if (error.code === "contact_required") {
          form.setError("contact", { message })
          form.setFocus("email")
        } else if (error.code === "trade_name_not_allowed" && company) {
          form.setError("tradeName", { message })
          form.setFocus("tradeName")
        } else {
          setFailure(error)
        }
      }
    },
    // O erro do contato não tem campo para o foco: sozinho, o foco vai ao e-mail.
    (errors) => {
      if (Object.keys(errors).every((name) => name === "contact")) form.setFocus("email")
    },
  )

  return { form, company, failure, submit }
}

type CustomerForm = ReturnType<typeof useCustomerForm>

function Lock({ id, children }: { id?: string; children: React.ReactNode }) {
  return (
    <p id={id} className={cn(HELP, "flex items-start gap-1.5")}>
      <LockIcon aria-hidden="true" strokeWidth={2.5} className="size-3.5 shrink-0" />
      {children}
    </p>
  )
}

/**
 * O documento do cadastro (telas 7 e 8): máscara ao digitar, o tipo reconhecido ao lado e o aviso
 * do cadeado (ADR-24). Teclado de texto: o CNPJ aceita letras (Tarefa 1). Com o documento já usado,
 * o erro leva à lista buscando por ele, com todos (ADR-23), no lugar do cadeado (tela 8).
 */
function DocumentField({ form }: { form: CustomerForm["form"] }) {
  const id = React.useId()
  const {
    field: { ref, name, value, onBlur, onChange },
    fieldState,
  } = useController({ control: form.control, name: "document" })
  const type = documentTypeOf(value)
  const error = fieldState.error
  const taken = error?.type === TAKEN

  return (
    <div className={FIELD}>
      <Label htmlFor={id}>CPF ou CNPJ *</Label>
      <div className="flex">
        <Input
          id={id}
          size="compact"
          type="text"
          autoComplete="off"
          aria-invalid={error ? true : undefined}
          aria-describedby={describedBy(
            error && `${id}-error`,
            type && `${id}-type`,
            !taken && `${id}-lock`,
            `${id}-help`,
          )}
          ref={ref}
          name={name}
          value={value}
          onBlur={onBlur}
          onChange={(event) => {
            const next = formatDocument(event.target.value)

            onChange(next)
            // Pessoa física não tem nome fantasia: o que estava nele não fica (ADR-21).
            if (documentTypeOf(next) === "CPF") form.setValue("tradeName", "")
          }}
        />
        {type && (
          <span
            id={`${id}-type`}
            className="flex items-center border border-l-0 border-line-strong bg-chip-ok px-3.5 text-body-sm font-semibold whitespace-nowrap"
          >
            {type}
          </span>
        )}
      </div>
      {error?.message && <FieldError id={`${id}-error`}>{error.message}</FieldError>}
      {taken ? (
        <Link
          to={`${LIST_PATH}?q=${normalizeDocument(value)}&situacao=ALL`}
          className="inline-flex min-h-11 items-center gap-1.5 self-start text-body-sm font-semibold text-brand-text"
        >
          Ver cliente com este documento
          <ChevronRightIcon aria-hidden="true" strokeWidth={2.5} className="size-4" />
        </Link>
      ) : (
        <Lock id={`${id}-lock`}>{NEW_LOCK}</Lock>
      )}
      <p id={`${id}-help`} className={cn(HELP, "hidden md:block")}>
        {DOCUMENT_HELP}
      </p>
    </div>
  )
}

/** O documento depois de salvo (tela 7, modo editar): texto em caixa apagada, com o tipo no selo. */
function FixedDocument({ customer }: { customer: CustomerDetail }) {
  return (
    <div className={FIELD}>
      <span className="text-label">CPF ou CNPJ</span>
      <div className="flex h-12 items-center justify-between border border-line bg-nav px-3 text-body text-text-muted md:h-11">
        <span>{formatDocument(customer.document)}</span>
        <span className="bg-chip-ok px-2 py-0.5 text-label leading-6 text-text">
          {customer.documentType}
        </span>
      </div>
      <Lock>{EDIT_LOCK}</Lock>
    </div>
  )
}

function TextField({
  form,
  name,
  label,
  help,
}: {
  form: CustomerForm["form"]
  name: "name" | "tradeName"
  label: string
  help?: string
}) {
  const id = React.useId()
  const error = form.formState.errors[name]?.message

  return (
    <div className={FIELD}>
      <Label htmlFor={id}>{label}</Label>
      <Input
        id={id}
        size="compact"
        type="text"
        autoComplete="off"
        maxLength={NAME_MAX}
        aria-invalid={error ? true : undefined}
        aria-describedby={describedBy(error && `${id}-error`, help && `${id}-help`)}
        {...form.register(name)}
      />
      {error && <FieldError id={`${id}-error`}>{error}</FieldError>}
      {help && (
        <p id={`${id}-help`} className={cn(HELP, "hidden md:block")}>
          {help}
        </p>
      )}
    </div>
  )
}

/** O telefone com a máscara brasileira (ADR-18); o estrangeiro guardado só aparece, desabilitado. */
function PhoneField({
  form,
  contactErrorId,
  foreignPhone,
}: {
  form: CustomerForm["form"]
  contactErrorId?: string
  foreignPhone?: string
}) {
  const id = React.useId()
  const {
    field: { ref, name, value, onBlur, onChange },
    fieldState,
  } = useController({ control: form.control, name: "phone" })
  const error = fieldState.error?.message

  if (foreignPhone)
    return (
      <div className={FIELD}>
        <Label htmlFor={id}>Telefone</Label>
        <Input
          id={id}
          size="compact"
          type="tel"
          value={foreignPhone}
          disabled
          readOnly
          aria-describedby={`${id}-help`}
        />
        <p id={`${id}-help`} className={HELP}>
          {FOREIGN_PHONE_HELP}
        </p>
      </div>
    )

  return (
    <div className={FIELD}>
      <Label htmlFor={id}>Telefone</Label>
      <Input
        id={id}
        size="compact"
        type="tel"
        inputMode="numeric"
        autoComplete="off"
        placeholder="(00) 00000-0000"
        aria-invalid={error || contactErrorId ? true : undefined}
        aria-describedby={describedBy(
          error ? `${id}-error` : contactErrorId,
          `${id}-help`,
        )}
        ref={ref}
        name={name}
        value={value}
        onBlur={onBlur}
        onChange={(event) => {
          onChange(maskPhone(event.target.value))
          form.clearErrors("contact")
        }}
      />
      {error && <FieldError id={`${id}-error`}>{error}</FieldError>}
      <p id={`${id}-help`} className={cn(HELP, "hidden md:block")}>
        {PHONE_HELP}
      </p>
    </div>
  )
}

/**
 * A seção Contato (ADR-16): a regra no título (tela 7, linhas 36 e 37) e o erro "pelo menos um" no
 * topo, com os dois campos marcados (tela 8, linhas 102 a 116). Na web, os dois lado a lado.
 */
function ContactSection({
  form,
  foreignPhone,
}: {
  form: CustomerForm["form"]
  foreignPhone?: string
}) {
  const id = React.useId()
  const contactError = form.formState.errors.contact?.message
  const contactErrorId = contactError ? `${id}-error` : undefined
  const emailError = form.formState.errors.email?.message

  return (
    <section
      aria-labelledby={`${id}-title`}
      className="flex flex-col gap-3 border-t border-line pt-4.5 md:pt-5"
    >
      <h2 id={`${id}-title`} className="text-label text-text-muted">
        {CONTACT_TITLE}
      </h2>
      {contactError && <FieldError id={`${id}-error`}>{contactError}</FieldError>}
      <div className="flex flex-col gap-3 md:grid md:grid-cols-2">
        <div className={FIELD}>
          <Label htmlFor={`${id}-email`}>E-mail</Label>
          <Input
            id={`${id}-email`}
            size="compact"
            type="email"
            autoComplete="off"
            maxLength={EMAIL_MAX}
            aria-invalid={emailError || contactError ? true : undefined}
            aria-describedby={emailError ? `${id}-email-error` : contactErrorId}
            {...form.register("email", { onChange: () => form.clearErrors("contact") })}
          />
          {emailError && <FieldError id={`${id}-email-error`}>{emailError}</FieldError>}
        </div>
        <PhoneField
          form={form}
          contactErrorId={contactErrorId}
          foreignPhone={foreignPhone}
        />
      </div>
    </section>
  )
}

/** Os campos, na ordem das telas 7 e 8: documento, nome, nome fantasia (só CNPJ) e contato. */
function CustomerFields({
  model,
  id,
  document,
  foreignPhone,
  footer,
}: {
  model: CustomerForm
  id: string
  document: React.ReactNode
  foreignPhone?: string
  footer?: React.ReactNode
}) {
  return (
    <form id={id} noValidate onSubmit={model.submit} className={FORM}>
      {model.failure && <Alert tone="danger">{errorMessage(model.failure)}</Alert>}
      {document}
      <TextField
        form={model.form}
        name="name"
        label={model.company ? "Razão social *" : "Nome *"}
      />
      {model.company && (
        <TextField
          form={model.form}
          name="tradeName"
          label="Nome fantasia (opcional)"
          help={TRADE_NAME_HELP}
        />
      )}
      <ContactSection form={model.form} foreignPhone={foreignPhone} />
      {footer}
    </form>
  )
}

/** "Cancelar" e o salvar, só na web (tela 7, linhas 43 a 47); no celular o salvar é o rodapé fixo. */
function WebButtons({
  cancelTo,
  children,
}: {
  cancelTo: string
  children: React.ReactNode
}) {
  return (
    <div className="flex justify-end gap-3">
      <Link
        to={cancelTo}
        className="cut-sm inline-flex min-h-11 items-center bg-chip-ok px-5 text-body-sm font-semibold text-text"
      >
        Cancelar
      </Link>
      {children}
    </div>
  )
}

/** A moldura e as ações comuns: o salvar vai no rodapé fixo no celular e ao lado de Cancelar na web. */
function FormPage({
  back,
  title,
  saveLabel,
  model,
  document,
  foreignPhone,
}: {
  back: Back
  title: string
  saveLabel: string
  model: CustomerForm
  document: React.ReactNode
  foreignPhone?: string
}) {
  const isDesktop = useMediaQuery(MD_UP)
  const formId = React.useId()
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

  return (
    <Frame back={back} title={title} mobileAction={!isDesktop && save}>
      <CustomerFields
        model={model}
        id={formId}
        document={document}
        foreignPhone={foreignPhone}
        footer={isDesktop && <WebButtons cancelTo={back.to}>{save}</WebButtons>}
      />
    </Frame>
  )
}

const LIST_BACK: Back = { to: LIST_PATH, label: "Clientes" }
const customerBack = (id: string): Back => ({
  to: customerPath(id),
  label: "Ficha do cliente",
})

/** Novo cliente (telas 7 e 8): salvar abre a ficha nova. */
function NewCustomer() {
  const navigate = useNavigate()
  const register = useRegisterCustomer()
  const model = useCustomerForm(
    { document: "", name: "", tradeName: "", email: "", phone: "" },
    async (input, values) => {
      const { id } = await register.mutateAsync({
        ...input,
        document: normalizeDocument(values.document),
      })
      void navigate(customerPath(id))
    },
  )

  return (
    <FormPage
      back={LIST_BACK}
      title="Novo cliente"
      saveLabel="Salvar cliente"
      model={model}
      document={<DocumentField form={model.form} />}
    />
  )
}

/**
 * Editar cliente (tela 7, modo editar): os valores do próprio cliente, o documento só leitura
 * (ADR-3, ADR-12) e fora do corpo do `PUT`. Salvar volta para a ficha.
 */
function EditCustomer({ customer }: { customer: CustomerDetail }) {
  const navigate = useNavigate()
  const update = useUpdateCustomer()
  const foreignPhone =
    customer.phone !== null && !isBrazilian(customer.phone) ? customer.phone : undefined
  const model = useCustomerForm(
    {
      document: customer.document,
      name: customer.name,
      tradeName: customer.tradeName ?? "",
      email: customer.email ?? "",
      phone: customer.phone === null ? "" : formatPhone(customer.phone),
    },
    async (input) => {
      await update.mutateAsync({ id: customer.id, input })
      void navigate(customerPath(customer.id))
    },
    foreignPhone,
  )

  return (
    <FormPage
      back={customerBack(customer.id)}
      title="Editar cliente"
      saveLabel="Salvar alterações"
      model={model}
      document={<FixedDocument customer={customer} />}
      foreignPhone={foreignPhone}
    />
  )
}

/**
 * Só monta o formulário com o cliente lido agora (o do cache pode ser de antes da última edição):
 * até lá não há campo nem Salvar. O desativado não se edita: volta para a ficha.
 */
function EditCustomerLoader({ id }: { id: string }) {
  const query = useCustomer(id)
  const customer = query.data
  const failure = query.error as Failure

  if (failure || !customer || !query.isFetchedAfterMount)
    return (
      <Frame back={customerBack(id)} title="Editar cliente">
        {failure ? <Alert tone="danger">{errorMessage(failure)}</Alert> : <Spinner />}
      </Frame>
    )

  if (customer.status === "DEACTIVATED") return <Navigate to={customerPath(id)} replace />

  return <EditCustomer key={customer.id} customer={customer} />
}

/**
 * `new` e `edit` do cliente (telas 7 e 8). Sem `canEdit` (o Técnico, ADR-7) o novo volta para a
 * lista e a edição, para a ficha.
 */
function CustomerFormScreen({
  mode,
  canEdit,
}: {
  mode: "new" | "edit"
  canEdit: boolean
}) {
  const { id = "" } = useParams()

  if (mode === "edit")
    return canEdit ? (
      <EditCustomerLoader id={id} />
    ) : (
      <Navigate to={customerPath(id)} replace />
    )

  if (!canEdit) return <Navigate to={LIST_PATH} replace />

  return <NewCustomer />
}

export { CustomerFormScreen }
