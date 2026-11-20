import { zodResolver } from "@hookform/resolvers/zod"
import { MailIcon } from "lucide-react"
import { useForm } from "react-hook-form"
import { Link } from "react-router"

import type { ApiError } from "@/shared/api/api-error"
import { cn } from "@/shared/lib/cn"
import { Alert } from "@/shared/ui/alert"
import { Button } from "@/shared/ui/button"
import { Form, FormField, FormItem, FormLabel, FormMessage } from "@/shared/ui/form"
import { Input } from "@/shared/ui/input"
import { Panel } from "@/shared/ui/panel"

import { AuthHeader, AuthShell } from "../components/auth-shell"
import { useRequestPasswordReset } from "../iam.hooks"
import { errorMessage } from "../iam.messages"
import {
  EMAIL_MAX,
  forgotPasswordSchema,
  type ForgotPasswordValues,
} from "../iam.schemas"

const TEXT_LINK =
  "inline-flex min-h-11 items-center self-start text-body-sm font-semibold"

/**
 * Esqueci minha senha (mockups "Esqueci minha senha" e "Link enviado"). O backend responde igual
 * exista o e-mail ou não, e a tela também: o aviso de sucesso nunca cita o e-mail digitado.
 */
function ForgotPassword() {
  const request = useRequestPasswordReset()
  const form = useForm<ForgotPasswordValues>({
    resolver: zodResolver(forgotPasswordSchema),
    defaultValues: { email: "" },
  })

  const error = request.error as Partial<ApiError> | null

  async function onSubmit(values: ForgotPasswordValues) {
    if (request.isPending) return

    try {
      await request.mutateAsync(values)
    } catch {
      // O erro já está em `request.error` e a tela o mostra.
    }
  }

  if (request.isSuccess) {
    return <LinkSent onRetry={() => request.reset()} />
  }

  return (
    <AuthShell
      compact
      title="Esqueceu a senha?"
      description="Informe o e-mail da sua conta. Se ele existir, enviamos um link para criar uma nova senha."
    >
      <Form {...form}>
        <form
          noValidate
          onSubmit={form.handleSubmit(onSubmit)}
          className="flex flex-col gap-5 md:gap-6"
        >
          {error && (
            <Alert tone={error.code === "rate_limited" ? "warning" : "danger"}>
              {errorMessage(error)}
            </Alert>
          )}

          <FormField
            control={form.control}
            name="email"
            render={({ field }) => (
              <FormItem className="gap-1.5">
                <FormLabel>E-mail</FormLabel>
                <Input
                  {...field}
                  type="email"
                  autoComplete="email"
                  maxLength={EMAIL_MAX}
                />
                <FormMessage />
              </FormItem>
            )}
          />

          <Button
            type="submit"
            size="lg"
            loading={request.isPending}
            className="md:min-h-12 md:cut-sm"
          >
            Enviar link
          </Button>
        </form>
      </Form>

      <Link to="/entrar" className={cn(TEXT_LINK, "text-brand-text underline")}>
        Voltar para o login
      </Link>
    </AuthShell>
  )
}

/** Aviso de link enviado: o mesmo desenho do painel de `LinkProblem`, com o envelope. */
function LinkSent({ onRetry }: { onRetry: () => void }) {
  return (
    <div className="flex min-h-dvh flex-col gap-6 px-6 pt-8 pb-6 md:gap-0 md:p-0">
      <AuthHeader />
      <main className="flex flex-1 flex-col md:items-center md:justify-center md:px-4 md:pt-8 md:pb-16">
        <div className="flex w-full max-w-110 flex-col gap-5">
          <Panel
            aria-labelledby="link-sent-title"
            className="flex flex-col gap-4 p-6 md:px-9 md:py-10"
          >
            <MailIcon aria-hidden="true" className="size-8 text-highlight" />
            <h1 id="link-sent-title" className="text-title">
              Confira seu e-mail
            </h1>
            <p className="text-body">
              Se o e-mail estiver cadastrado, enviamos um link para criar uma nova senha.
              O link vale por 1 hora.
            </p>
          </Panel>
          <Button
            variant="outline"
            size="lg"
            nativeButton={false}
            render={<Link to="/entrar" />}
            role="link"
            className="w-full"
          >
            Voltar para o login
          </Button>
          <Button
            type="button"
            variant="link"
            onClick={onRetry}
            className={cn(TEXT_LINK, "text-body-sm")}
          >
            Pedir o link de novo
          </Button>
        </div>
      </main>
    </div>
  )
}

export { ForgotPassword }
