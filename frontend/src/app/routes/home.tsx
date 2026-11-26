import { Navigate } from "react-router"

/**
 * A raiz não tem tela própria: leva todos os papéis para Clientes, o primeiro item do menu
 * (ADR-26). O Técnico também vai: ele consulta a lista. A página inicial provisória acabou com
 * a primeira feature de negócio.
 */
export default function Home() {
  return <Navigate to="/clientes" replace />
}
