import * as React from "react"
import { Dialog } from "@base-ui/react/dialog"
import { zodResolver } from "@hookform/resolvers/zod"
import { useForm } from "react-hook-form"
import { useNavigate } from "react-router"
import { toast } from "sonner"

import { UNKNOWN_ERROR_MESSAGE, type ApiError } from "@/shared/api/api-error"
import { cn } from "@/shared/lib/cn"
import { useMediaQuery } from "@/shared/lib/use-media-query"
import { Alert } from "@/shared/ui/alert"
import { Button } from "@/shared/ui/button"
import { Form, FormField, FormItem, FormLabel, FormMessage } from "@/shared/ui/form"
import { Input } from "@/shared/ui/input"

import { MobileHeader } from "../components/mobile-header"
import { RolePicker } from "../components/role-picker"
import { useInviteUser } from "../iam.hooks"
import { errorMessage, messageFor } from "../iam.messages"
import { inviteUserSchema, type InviteUserValues } from "../iam.schemas"

const EMAIL_TAKEN = "email_already_registered"
const USERS_PATH = "/usuarios"

// Texto aprovado do mockup "Convidar usuário".
const INTRO =
  "O convite chega por e-mail e vale por um tempo limitado. A pessoa define a própria senha."

/**
 * Formulário do convite: nome (2 a 80), e-mail (até 254) e papel. "Enviar convite" só liga com
 * tudo válido. `email_already_registered` sai no campo e-mail; o resto, no alerta. Sucesso
 * atualiza a lista (o hook invalida `["users"]`), avisa e chama `onDone`.
 */
function useInviteForm(onDone: () => void) {
  const invite = useInviteUser()
  const form = useForm<InviteUserValues>({
    resolver: zodResolver(inviteUserSchema),
    mode: "onChange",
    defaultValues: { name: "", email: "" },
  })

  // O envio é assíncrono (validação antes do pedido): a trava é um ref, não o estado do hook.
  const sending = React.useRef(false)

  async function submit(values: InviteUserValues) {
    if (sending.current) return

    sending.current = true
    try {
      await invite.mutateAsync(values)
      // "Convite enviado" é o aviso de sucesso aprovado (telas-complementares.md, sem artboard).
      toast.success("Convite enviado")
      onDone()
    } catch (e) {
      if ((e as Partial<ApiError>).code === EMAIL_TAKEN) {
        form.setError("email", {
          message: messageFor(EMAIL_TAKEN, UNKNOWN_ERROR_MESSAGE),
        })
        form.setFocus("email")
      }
      // Os demais erros chegam em `invite.error` e o formulário os mostra no alerta.
    } finally {
      sending.current = false
    }
  }

  return {
    form,
    invite,
    onSubmit: (event: React.FormEvent) => form.handleSubmit(submit)(event),
  }
}

type InviteForm = ReturnType<typeof useInviteForm>

function Fields({ form, invite, onSubmit, id }: InviteForm & { id: string }) {
  const error = invite.error as Partial<ApiError> | null

  return (
    <Form {...form}>
      <form
        id={id}
        noValidate
        onSubmit={onSubmit}
        className="flex flex-col gap-4 md:**:data-[slot=input]:bg-ground"
      >
        {error && error.code !== EMAIL_TAKEN && (
          <Alert tone="danger">{errorMessage(error)}</Alert>
        )}
        <FormField
          control={form.control}
          name="name"
          render={({ field }) => (
            <FormItem className="gap-1.5">
              <FormLabel>Nome completo</FormLabel>
              <Input {...field} type="text" autoComplete="off" />
              <FormMessage />
            </FormItem>
          )}
        />
        <FormField
          control={form.control}
          name="email"
          render={({ field }) => (
            <FormItem className="gap-1.5">
              <FormLabel>E-mail</FormLabel>
              <Input {...field} type="email" autoComplete="off" />
              <FormMessage />
            </FormItem>
          )}
        />
        <FormField
          control={form.control}
          name="role"
          render={({ field }) => (
            <RolePicker value={field.value} onChange={field.onChange} />
          )}
        />
      </form>
    </Form>
  )
}

/** Cinza e sem opacidade até o formulário estar pronto, como os botões das telas de senha. */
function SendButton({
  id,
  valid,
  loading,
  className,
  size,
}: {
  id: string
  valid: boolean
  loading: boolean
  className?: string
  size?: "md" | "lg"
}) {
  return (
    <Button
      type="submit"
      form={id}
      size={size}
      disabled={!valid}
      loading={loading}
      className={cn(className, !valid && "bg-line text-text-muted disabled:opacity-100")}
    >
      Enviar convite
    </Button>
  )
}

/**
 * Convidar usuário (mockups "Convidar usuário · web" e "· celular"). Rota irmã de `:id` sob
 * `/usuarios`. Na web é um diálogo modal sobre a lista (Base UI: foco preso, Esc fecha, o foco
 * volta ao botão que abriu); no celular, a tela cheia com cabeçalho próprio (com "Voltar") e o
 * botão no rodapé. Fechar ou concluir volta para `/usuarios`.
 */
function InviteUserScreen() {
  const isDesktop = useMediaQuery("(min-width: 48rem)")
  const navigate = useNavigate()
  const formId = React.useId()
  const close = () => void navigate(USERS_PATH)
  const invite = useInviteForm(close)
  const sending = invite.invite.isPending
  const valid = invite.form.formState.isValid

  if (isDesktop)
    return (
      <Dialog.Root open onOpenChange={(open) => !open && close()}>
        <Dialog.Portal>
          <Dialog.Backdrop className="fixed inset-0 bg-ground/80" />
          <Dialog.Popup
            aria-modal="true"
            className="cut-lg fixed top-1/2 left-1/2 flex max-h-[calc(100dvh-3rem)] overflow-y-auto w-[min(30rem,calc(100%-3rem))] -translate-x-1/2 -translate-y-1/2 flex-col gap-6 bg-panel p-8"
          >
            <div className="flex flex-col gap-2">
              <Dialog.Title render={<h1 />} className="text-title">
                Convidar usuário
              </Dialog.Title>
              <Dialog.Description className="text-body-sm text-text-muted">
                {INTRO}
              </Dialog.Description>
            </div>
            <Fields {...invite} id={formId} />
            <div className="flex flex-wrap justify-end gap-3">
              <Button
                variant="outline"
                onClick={close}
                className="md:min-h-12 [&>span]:bg-panel"
              >
                Cancelar
              </Button>
              <SendButton
                id={formId}
                valid={valid}
                loading={sending}
                className="md:min-h-12 md:px-6"
              />
            </div>
          </Dialog.Popup>
        </Dialog.Portal>
      </Dialog.Root>
    )

  return (
    <>
      <MobileHeader to={USERS_PATH} title="Convidar usuário" />
      <div className="flex flex-col gap-4 px-4 pt-5 pb-28">
        <p className="text-body-sm text-text-muted">{INTRO}</p>
        <Fields {...invite} id={formId} />
      </div>
      <div className="fixed inset-x-0 bottom-0 border-t border-line bg-nav px-4 pt-3 pb-5">
        <SendButton
          id={formId}
          size="lg"
          valid={valid}
          loading={sending}
          className="w-full"
        />
      </div>
    </>
  )
}

export { InviteUserScreen }
