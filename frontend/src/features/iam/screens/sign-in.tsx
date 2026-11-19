import { zodResolver } from "@hookform/resolvers/zod"
import { useForm } from "react-hook-form"
import { Link, useNavigate } from "react-router"

import { UNKNOWN_ERROR_MESSAGE, type ApiError } from "@/shared/api/api-error"
import { cn } from "@/shared/lib/cn"
import { Alert } from "@/shared/ui/alert"
import { Button } from "@/shared/ui/button"
import { Form, FormField, FormItem, FormLabel, FormMessage } from "@/shared/ui/form"
import { Input } from "@/shared/ui/input"

import { AuthShell } from "../components/auth-shell"
import { PasswordField } from "../components/password-field"
import { useLogin } from "../iam.hooks"
import { messageFor } from "../iam.messages"
import { signInSchema, type SignInValues } from "../iam.schemas"

const INVALID_CREDENTIALS =
  "E-mail ou senha incorretos. Confira os dados e tente de novo."
const RATE_LIMITED_TITLE = "Muitas tentativas de entrada"
const RATE_LIMITED_TEXT =
  "Por segurança, o acesso fica pausado por 15 minutos. Tente de novo depois desse prazo."

/** As mensagens da API vêm em minúscula; no aviso a frase começa com maiúscula. */
const sentence = (text: string) => text.charAt(0).toUpperCase() + text.slice(1)

/**
 * Entrar (mockups "Entrar · web", "senha errada" e "bloqueado"). O `device` não vai no corpo:
 * o backend o deriva dos cabeçalhos. Todo `invalid_credentials` mostra a mesma mensagem, sem
 * usar o texto da API, para não revelar quem tem conta. O bloqueio (`rate_limited`) desliga os
 * campos e o botão e nunca tenta de novo sozinho.
 */
function SignIn() {
  const navigate = useNavigate()
  const login = useLogin()
  const form = useForm<SignInValues>({
    resolver: zodResolver(signInSchema),
    defaultValues: { email: "", password: "" },
  })

  const error = login.error as Partial<ApiError> | null
  const locked = error?.code === "rate_limited"

  async function onSubmit(values: SignInValues) {
    if (login.isPending || locked) return

    try {
      const session = await login.mutateAsync(values)
      await navigate(session.mustChangePassword ? "/conta/trocar-senha" : "/")
    } catch {
      // O erro já está em `login.error` e a tela o mostra.
    }
  }

  return (
    <AuthShell
      title="Entrar"
      description="Acesse o sistema da DUNAS com o e-mail do seu convite."
      footer="O acesso é por convite. Se você ainda não tem conta, peça ao administrador."
    >
      <Form {...form}>
        <form
          noValidate
          onSubmit={form.handleSubmit(onSubmit)}
          className="flex flex-col gap-6"
        >
          {locked ? (
            <Alert tone="warning" title={RATE_LIMITED_TITLE}>
              {RATE_LIMITED_TEXT}
            </Alert>
          ) : (
            error && (
              <Alert tone="danger">
                {error.code === "invalid_credentials"
                  ? INVALID_CREDENTIALS
                  : sentence(
                      messageFor(error.code, error.message || UNKNOWN_ERROR_MESSAGE),
                    )}
              </Alert>
            )
          )}

          <div className="flex flex-col gap-4">
            <FormField
              control={form.control}
              name="email"
              render={({ field }) => (
                <FormItem className="gap-1.5">
                  <FormLabel className={cn(locked && "text-text-muted")}>
                    E-mail
                  </FormLabel>
                  <Input
                    {...field}
                    type="email"
                    autoComplete="username"
                    placeholder="nome@empresa.com.br"
                    disabled={locked}
                  />
                  <FormMessage />
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name="password"
              render={({ field, fieldState }) => (
                <PasswordField
                  {...field}
                  id="password"
                  label="Senha"
                  autoComplete="current-password"
                  error={fieldState.error?.message}
                  disabled={locked}
                />
              )}
            />
          </div>

          <Button
            type="submit"
            size="lg"
            loading={login.isPending}
            disabled={locked}
            className={cn(
              "md:min-h-12 md:cut-sm",
              locked &&
                "bg-line text-text-muted disabled:opacity-100 hover:brightness-100",
            )}
          >
            Entrar
          </Button>
        </form>
      </Form>

      <Link
        to="/esqueci-senha"
        className="inline-flex min-h-11 items-center self-start text-body-sm font-semibold text-brand-text underline"
      >
        Esqueci minha senha
      </Link>
    </AuthShell>
  )
}

export { SignIn }
