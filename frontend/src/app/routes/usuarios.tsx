import { RequireRole } from "@/features/iam/guards"
import { UsersScreen } from "@/features/iam/screens/users"

/**
 * `/usuarios`: a lista, só do Administrador (ADR-5). É a rota de layout de `usuarios.$id`: o
 * guarda cobre a lista e o detalhe, e a tela renderiza o `Outlet` na região de detalhe.
 */
export default function Usuarios() {
  return (
    <RequireRole role="ADMINISTRATOR">
      <UsersScreen />
    </RequireRole>
  )
}
