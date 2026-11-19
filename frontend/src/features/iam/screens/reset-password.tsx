import { zodResolver } from "@hookform/resolvers/zod"
import { useForm, useWatch } from "react-hook-form"
import { useNavigate, useSearchParams } from "react-router"
import { toast } from "sonner"

import { UNKNOWN_ERROR_MESSAGE, type ApiError } from "@/shared/api/api-error"
import { cn } from "@/shared/lib/cn"
import { Alert } from "@/shared/ui/alert"
import { Button } from "@/shared/ui/button"
import { Form, FormField, FormItem, FormLabel, FormMessage } from "@/shared/ui/form"
import { Input } from "@/shared/ui/input"

import { AuthShell } from "../components/auth-shell"
import { PasswordChecklist } from "../components/password-checklist"
import { PasswordField } from "../components/password-field"
import { useResetPassword } from "../iam.hooks"
import { messageFor } from "../iam.messages"
import { resetPasswordSchema, type ResetPasswordValues } from "../iam.schemas"
import { PASSWORD_MAX } from "../password-rules"
import { LinkProblem } from "./link-problem"

/** As mensagens da API vêm em minúscula; no aviso a frase começa com maiúscula. */
const sentence = (text: string) => text.charAt(0).toUpperCase() + text.slice(1)

/**
 * Redefinir senha (mockup "Redefinir senha, celular"). Como no convite, a tela não consulta o
 * token antes do envio; sem `token` na URL (ou vazio) cai direto no aviso de link usado, sem
 * chamar a API. A redefinição não abre sessão: vai para o login.
 */
function ResetPasswordScreen() {
  const [params] = useSearchParams()
  const token = params.get("token")

  return token ? (
    <ResetPasswordForm token={token} />
  ) : (
    <LinkProblem kind="used" source="reset" />
  )
}

function ResetPasswordForm({ token }: { token: string }) {
  const navigate = useNavigate()
  const reset = useResetPassword()
  const form = useForm<ResetPasswordValues>({
    resolver: zodResolver(resetPasswordSchema),
    mode: "onChange",
    defaultValues: { password: "", repeat: "" },
  })
  const password = useWatch({ control: form.control, name: "password" })

  const error = reset.error as Partial<ApiError> | null

  if (error?.code === "link_expired") return <LinkProblem kind="expired" source="reset" />
  if (error?.code === "link_used") return <LinkProblem kind="used" source="reset" />

  async function onSubmit(values: ResetPasswordValues) {
    if (reset.isPending) return

    try {
      await reset.mutateAsync({ token, password: values.password })
      // "Senha alterada" é o aviso de sucesso aprovado (telas-complementares.md, sem artboard).
      toast.success("Senha alterada")
      await navigate("/entrar")
    } catch {
      // O erro já está em `reset.error` e a tela o mostra.
    }
  }

  const ready = form.formState.isValid

  return (
    <AuthShell
      compact
      title="Crie uma nova senha"
      description="Defina uma senha para voltar a entrar."
    >
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
            loading={reset.isPending}
            disabled={!ready}
            className={cn(
              "md:min-h-12 md:cut-sm",
              !ready && "bg-line text-text-muted disabled:opacity-100",
            )}
          >
            Salvar nova senha
          </Button>
        </form>
      </Form>
    </AuthShell>
  )
}

export { ResetPasswordScreen }
