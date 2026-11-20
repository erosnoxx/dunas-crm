import { useNavigate, useSearchParams } from "react-router"

import { NewPasswordForm } from "../components/new-password-form"
import { useAcceptInvitation } from "../iam.hooks"
import { LinkProblem } from "../components/link-problem"

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

  return (
    <NewPasswordForm
      title="Crie sua senha"
      description="Defina uma senha para entrar."
      submitLabel="Criar senha e entrar"
      linkSource="invitation"
      isPending={accept.isPending}
      error={accept.error}
      onSubmit={async (password) => {
        // A sessão já fica aberta pelo cookie do servidor; o hook invalida `["me"]`.
        await accept.mutateAsync({ token, password })
        await navigate("/")
      }}
    />
  )
}

export { AcceptInvitationScreen }
