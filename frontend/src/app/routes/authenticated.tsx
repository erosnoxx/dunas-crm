import { Outlet } from "react-router"

import { RequireAuth, RequireSettledPassword } from "@/features/iam/guards"

/** Tudo que exige sessão: sem ela vai para o login; com troca de senha obrigatória, só a troca abre. */
export default function Authenticated() {
  return (
    <RequireAuth>
      <RequireSettledPassword>
        <Outlet />
      </RequireSettledPassword>
    </RequireAuth>
  )
}
