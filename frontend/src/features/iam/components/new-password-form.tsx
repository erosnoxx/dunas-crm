import { zodResolver } from "@hookform/resolvers/zod"
import { useForm } from "react-hook-form"

import { UNKNOWN_ERROR_MESSAGE, type ApiError } from "@/shared/api/api-error"
import { Alert } from "@/shared/ui/alert"
import { Form } from "@/shared/ui/form"

import { messageFor, sentence } from "../iam.messages"
import { acceptInvitationSchema, type AcceptInvitationValues } from "../iam.schemas"
import { LinkProblem } from "../screens/link-problem"
import { AuthShell } from "./auth-shell"
import { NewPasswordFields, PasswordSubmitButton } from "./new-password-fields"

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
            <NewPasswordFields name="password" />
          </div>

          <PasswordSubmitButton ready={ready} loading={isPending}>
            {submitLabel}
          </PasswordSubmitButton>
        </form>
      </Form>
    </AuthShell>
  )
}

export { NewPasswordForm }
