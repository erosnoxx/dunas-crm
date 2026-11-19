import * as React from "react"
import { zodResolver } from "@hookform/resolvers/zod"
import { ChevronLeftIcon } from "lucide-react"
import { useForm } from "react-hook-form"
import { Link, useNavigate } from "react-router"
import { toast } from "sonner"

import { UNKNOWN_ERROR_MESSAGE, type ApiError } from "@/shared/api/api-error"
import { Alert } from "@/shared/ui/alert"
import { Form, FormField } from "@/shared/ui/form"
import { Panel } from "@/shared/ui/panel"

import { AuthShell } from "../components/auth-shell"
import {
  NewPasswordFields,
  PasswordSubmitButton,
} from "../components/new-password-fields"
import { PasswordField } from "../components/password-field"
import { useChangePassword, useCurrentUser, useLogout } from "../iam.hooks"
import { messageFor, sentence } from "../iam.messages"
import { changePasswordSchema, type ChangePasswordValues } from "../iam.schemas"

const WRONG_CURRENT = "wrong_current_password"

/**
 * Trocar senha (mockups "Trocar senha, celular" e "Trocar a senha inicial, celular"). A variante
 * é a da sessão ao abrir a tela: com `mustChangePassword` é a obrigatória (ADR-2), em tela cheia,
 * sem voltar nem navegação, só "Sair"; sem ele é a voluntária, com "voltar", sem casca própria:
 * a casca autenticada (Tarefa 9) a envolve. Ao trocar, o hook atualiza `["me"]`, o que libera a
 * `RequireSettledPassword`, e a tela vai para `/`.
 */
function ChangePasswordScreen() {
  const { data: me } = useCurrentUser()

  return me ? <ChangePassword initialMandatory={me.mustChangePassword} /> : null
}

function ChangePassword({ initialMandatory }: { initialMandatory: boolean }) {
  // Fixa a variante ao abrir: o `me` atualizado pela troca não vira a obrigatória em voluntária
  // no meio da saída.
  const [mandatory] = React.useState(initialMandatory)
  const navigate = useNavigate()
  const change = useChangePassword()
  const logout = useLogout()
  const form = useForm<ChangePasswordValues>({
    resolver: zodResolver(changePasswordSchema),
    mode: "onChange",
    defaultValues: { currentPassword: "", newPassword: "", repeat: "" },
  })

  const error = change.error as Partial<ApiError> | null

  async function submit({ currentPassword, newPassword }: ChangePasswordValues) {
    if (change.isPending) return

    try {
      await change.mutateAsync({ currentPassword, newPassword })
      // "Senha alterada" é o aviso de sucesso aprovado (telas-complementares.md, sem artboard).
      toast.success("Senha alterada")
      await navigate("/")
    } catch (e) {
      if ((e as Partial<ApiError>).code === WRONG_CURRENT)
        form.setError("currentPassword", {
          message: messageFor(WRONG_CURRENT, UNKNOWN_ERROR_MESSAGE),
        })
      // Os demais erros chegam em `change.error` e a tela os mostra no alerta.
    }
  }

  const content = (
    <Form {...form}>
      <form
        noValidate
        onSubmit={form.handleSubmit(submit)}
        className="flex flex-col gap-5 md:gap-6"
      >
        {error && error.code !== WRONG_CURRENT && (
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
            name="currentPassword"
            render={({ field }) => (
              <PasswordField
                {...field}
                id="currentPassword"
                label="Senha atual"
                autoComplete="current-password"
                error={form.formState.errors.currentPassword?.message}
              />
            )}
          />
          <NewPasswordFields name="newPassword" />
        </div>

        <PasswordSubmitButton ready={form.formState.isValid} loading={change.isPending}>
          {mandatory ? "Salvar e continuar" : "Salvar nova senha"}
        </PasswordSubmitButton>
      </form>
    </Form>
  )

  if (mandatory)
    return (
      <AuthShell
        compact
        title="Troque a senha inicial"
        description="Sua conta foi criada com uma senha provisória. Defina a sua para continuar."
        footer={
          <button
            type="button"
            disabled={logout.isPending}
            onClick={() => logout.mutate()}
            className="min-h-11 text-body-sm font-semibold text-brand-text underline md:min-h-0"
          >
            Sair
          </button>
        }
      >
        {content}
      </AuthShell>
    )

  return (
    <div className="flex flex-col">
      <header className="flex h-16 items-center gap-1 border-b border-line bg-nav px-2 md:border-0 md:bg-transparent md:px-6">
        <Link
          to="/"
          aria-label="Voltar"
          className="flex size-12 items-center justify-center"
        >
          <ChevronLeftIcon aria-hidden="true" className="size-6" />
        </Link>
        <h1 className="text-title">Trocar senha</h1>
      </header>
      <main className="flex flex-col px-4 pt-5 pb-6 md:items-center md:pt-8">
        <Panel className="w-full max-w-110 max-md:bg-transparent max-md:[clip-path:none] md:px-9 md:py-10 md:**:data-[slot=input]:bg-ground">
          {content}
        </Panel>
      </main>
    </div>
  )
}

export { ChangePasswordScreen }
