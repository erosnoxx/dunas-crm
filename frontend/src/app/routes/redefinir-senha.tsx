import { ResetPasswordScreen } from "@/features/iam/screens/reset-password"

/**
 * `/redefinir-senha?token=`: pública. Sem `RedirectIfAuthenticated`, como `/convite`: o link é de
 * uma pessoa, e redirecionar quem já tem sessão engoliria o link sem aviso.
 */
export default function RedefinirSenha() {
  return <ResetPasswordScreen />
}
