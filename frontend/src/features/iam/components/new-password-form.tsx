import { zodResolver } from "@hookform/resolvers/zod"
import { useForm, useWatch } from "react-hook-form"

import { UNKNOWN_ERROR_MESSAGE, type ApiError } from "@/shared/api/api-error"
import { cn } from "@/shared/lib/cn"
import { Alert } from "@/shared/ui/alert"
import { Button } from "@/shared/ui/button"
import { Form, FormField, FormItem, FormLabel, FormMessage } from "@/shared/ui/form"
import { Input } from "@/shared/ui/input"

import { messageFor, sentence } from "../iam.messages"
import { acceptInvitationSchema, type AcceptInvitationValues } from "../iam.schemas"
import { PASSWORD_MAX } from "../password-rules"
import { LinkProblem } from "../screens/link-problem"
import { AuthShell } from "./auth-shell"
import { PasswordChecklist } from "./password-checklist"
import { PasswordField } from "./password-field"

type NewPasswordFormProps = {
  title: string
  description: string
  submitLabel: string
  /** O aviso de link que `link_expired`/`link_used` da API trocam pela tela. */
  linkSource: "invitation" | "reset"
  isPending: boolean
  error: unknown
  /** Recebe a senha como digitada (nunca aparada). Rejeitar é ok: o erro chega por `error`. */
  onSubmit: (password: string) => Promise<unknown>
}

/**
 * Tela de definir uma senha nova (convite e redefinição): nova senha com contador e checklist,
 * repetição e botão que só liga com as 5 regras e a repetição iguais. O mesmo schema vale para
 * as duas telas.
 */
function NewPasswordForm({
  title,
  description,
  submitLabel,
  linkSource,
  isPending,
  error: rawError,
  onSubmit,
}: NewPasswordFormProps) {
  // `onChange` + resolver: `isValid` só liga com as 5 regras e a repetição iguais.
  const form = useForm<AcceptInvitationValues>({
    resolver: zodResolver(acceptInvitationSchema),
    mode: "onChange",
    defaultValues: { password: "", repeat: "" },
  })
  const password = useWatch({ control: form.control, name: "password" })

  const error = rawError as Partial<ApiError> | null

  if (error?.code === "link_expired")
    return <LinkProblem kind="expired" source={linkSource} />
  if (error?.code === "link_used") return <LinkProblem kind="used" source={linkSource} />

  async function submit(values: AcceptInvitationValues) {
    if (isPending) return

    try {
      await onSubmit(values.password)
    } catch {
      // O erro já chegou em `error` e a tela o mostra.
    }
  }

  const ready = form.formState.isValid

  return (
    <AuthShell compact title={title} description={description}>
      <Form {...form}>
        <form
          noValidate
          onSubmit={form.handleSubmit(submit)}
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
            loading={isPending}
            disabled={!ready}
            className={cn(
              "md:min-h-12 md:cut-sm",
              !ready && "bg-line text-text-muted disabled:opacity-100",
            )}
          >
            {submitLabel}
          </Button>
        </form>
      </Form>
    </AuthShell>
  )
}

export { NewPasswordForm }
