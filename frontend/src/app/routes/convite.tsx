import { AcceptInvitationScreen } from "@/features/iam/screens/accept-invitation"

/**
 * `/convite?token=`: pública. Sem `RedirectIfAuthenticated` de propósito: o link é de uma
 * pessoa, e redirecionar quem já tem sessão engoliria o convite sem aviso.
 */
export default function Convite() {
  return <AcceptInvitationScreen />
}
