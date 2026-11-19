import { ChangePasswordScreen } from "@/features/iam/screens/change-password"

/**
 * `/conta/trocar-senha`: dentro do layout autenticado. É a única rota que a sessão com troca de
 * senha obrigatória abre (`RequireSettledPassword`).
 */
export default function ContaTrocarSenha() {
  return <ChangePasswordScreen />
}
