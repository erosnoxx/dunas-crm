import { useNavigate, useSearchParams } from "react-router"
import { toast } from "sonner"

import { NewPasswordForm } from "../components/new-password-form"
import { useResetPassword } from "../iam.hooks"
import { LinkProblem } from "./link-problem"

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

  return (
    <NewPasswordForm
      title="Crie uma nova senha"
      description="Defina uma senha para voltar a entrar."
      submitLabel="Salvar nova senha"
      linkSource="reset"
      isPending={reset.isPending}
      error={reset.error}
      onSubmit={async (password) => {
        await reset.mutateAsync({ token, password })
        // "Senha alterada" é o aviso de sucesso aprovado (telas-complementares.md, sem artboard).
        toast.success("Senha alterada")
        await navigate("/entrar")
      }}
    />
  )
}

export { ResetPasswordScreen }
