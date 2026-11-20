import { Outlet } from "react-router"

import { RequireAuth, RequireSettledPassword } from "@/features/iam/guards"

import { AppShell } from "../layout/app-shell"

/**
 * Tudo que exige sessão: sem ela vai para o login; com troca de senha obrigatória, só a troca
 * abre, e sem casca. As demais rotas ficam dentro da casca autenticada.
 */
export default function Authenticated() {
  return (
    <RequireAuth>
      <RequireSettledPassword>
        <AppShell>
          <Outlet />
        </AppShell>
      </RequireSettledPassword>
    </RequireAuth>
  )
}
