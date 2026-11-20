import { Panel } from "@/shared/ui/panel"

/**
 * Encaixe do detalhe do usuário: `/usuarios/:id` renderiza isto na região ao lado da lista (web)
 * ou na tela inteira (celular). A tarefa do painel troca o miolo por `UserPanel`, lendo o `id`
 * de `useParams`; a rota e a região já estão prontas.
 */
function UserDetailSlot() {
  return <Panel aria-label="Detalhe do usuário" className="p-6" />
}

export { UserDetailSlot }
