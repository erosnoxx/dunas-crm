import { RedirectIfAuthenticated } from "@/features/iam/guards"
import { SignIn } from "@/features/iam/screens/sign-in"

/** `/entrar`: pública; quem já tem sessão vai para o início. */
export default function Entrar() {
  return (
    <RedirectIfAuthenticated>
      <SignIn />
    </RedirectIfAuthenticated>
  )
}
