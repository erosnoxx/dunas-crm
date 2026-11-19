import { zodResolver } from "@hookform/resolvers/zod"
import { useForm, useWatch } from "react-hook-form"
import { useNavigate, useSearchParams } from "react-router"

import { UNKNOWN_ERROR_MESSAGE, type ApiError } from "@/shared/api/api-error"
import { cn } from "@/shared/lib/cn"
import { Alert } from "@/shared/ui/alert"
import { Button } from "@/shared/ui/button"
import { Form, FormField, FormItem, FormLabel, FormMessage } from "@/shared/ui/form"
import { Input } from "@/shared/ui/input"

import { AuthShell } from "../components/auth-shell"
import { PasswordChecklist } from "../components/password-checklist"
import { PasswordField } from "../components/password-field"
import { useAcceptInvitation } from "../iam.hooks"
import { messageFor } from "../iam.messages"
import { acceptInvitationSchema, type AcceptInvitationValues } from "../iam.schemas"
import { PASSWORD_MAX } from "../password-rules"
import { LinkProblem } from "./link-problem"

/** As mensagens da API vêm em minúscula; no aviso a frase começa com maiúscula. */
const sentence = (text: string) => text.charAt(0).toUpperCase() + text.slice(1)

/**
 * Convite (mockup "Convite · definir senha"). A tela não consulta o token antes de enviar a
 * senha (ADR-12): só o envio revela se o link vale. Sem `token` na URL (ou vazio) cai direto no
 * aviso de link usado, sem chamar a API.
 */
function AcceptInvitationScreen() {
  const [params] = useSearchParams()
  const token = params.get("token")

  return token ? <AcceptInvitationForm token={token} /> : <LinkProblem kind="used" />
}

function AcceptInvitationForm({ token }: { token: string }) {
  const navigate = useNavigate()
  const accept = useAcceptInvitation()
  // `onChange` + resolver: `isValid` só liga com as 5 regras e a repetição iguais.
  const form = useForm<AcceptInvitationValues>({
    resolver: zodResolver(acceptInvitationSchema),
    mode: "onChange",
    defaultValues: { password: "", repeat: "" },
  })
  const password = useWatch({ control: form.control, name: "password" })

  const error = accept.error as Partial<ApiError> | null

  if (error?.code === "link_expired") return <LinkProblem kind="expired" />
  if (error?.code === "link_used") return <LinkProblem kind="used" />

  async function onSubmit(values: AcceptInvitationValues) {
    if (accept.isPending) return

    try {
      // A sessão já fica aberta pelo cookie do servidor; o hook invalida `["me"]`.
      await accept.mutateAsync({ token, password: values.password })
      await navigate("/")
    } catch {
      // O erro já está em `accept.error` e a tela o mostra.
    }
  }

  const ready = form.formState.isValid

  return (
    <AuthShell compact title="Crie sua senha" description="Defina uma senha para entrar.">
      <Form {...form}>
        <form
          noValidate
          onSubmit={form.handleSubmit(onSubmit)}
          className="flex flex-col gap-5 md:gap-6"
        >
          {error && (
            <Alert tone="danger">
              {sentence(
                // `invalid_password` não está na tabela: a API diz o primeiro critério que falta.
                messageFor(error.code, error.message || UNKNOWN_ERROR_MESSAGE),
              )}
            </Alert>
          )}

          <div className="flex flex-col gap-4">
            <FormField
              control={form.control}
              name="password"
              render={({ field }) => (
                <PasswordField
                  {...field}
                  id="password"
                  label="Nova senha"
                  autoComplete="new-password"
                  showCount
                  onChange={(event) => {
                    field.onChange(event)
                    // A repetição depende da senha: revalida para o erro não ficar velho.
                    if (form.getValues("repeat")) void form.trigger("repeat")
                  }}
                />
              )}
            />
            <PasswordChecklist password={password} />
            <FormField
              control={form.control}
              name="repeat"
              render={({ field }) => (
                <FormItem className="gap-1.5">
                  <FormLabel>Repita a senha</FormLabel>
                  <Input
                    {...field}
                    type="password"
                    autoComplete="new-password"
                    maxLength={PASSWORD_MAX}
                  />
                  <FormMessage />
                </FormItem>
              )}
            />
          </div>

          <Button
            type="submit"
            size="lg"
            loading={accept.isPending}
            disabled={!ready}
            className={cn(
              "md:min-h-12 md:cut-sm",
              !ready && "bg-line text-text-muted disabled:opacity-100",
            )}
          >
            Criar senha e entrar
          </Button>
        </form>
      </Form>
    </AuthShell>
  )
}

export { AcceptInvitationScreen }
