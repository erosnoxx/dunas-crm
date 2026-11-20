import { Navigate } from "react-router"

import { useCurrentUser } from "@/features/iam/iam.hooks"
import { Panel } from "@/shared/ui/panel"

/**
 * Página inicial provisória (mockup "Navegação para Comercial e Técnico, web"): o Administrador
 * vai direto para os usuários; os demais ainda não têm tela de trabalho. A primeira feature de
 * negócio a substitui. No celular o título "Início" já está na barra superior da casca.
 */
export default function Home() {
  const { data: me } = useCurrentUser()

  if (!me) return null
  if (me.role === "ADMINISTRATOR") return <Navigate to="/usuarios" replace />

  return (
    <main className="flex flex-col gap-6 px-4 py-4 md:px-10 md:py-8">
      <h1 className="text-display-lg max-md:sr-only">Início</h1>
      <Panel className="flex flex-col gap-2 p-6">
        <h2 className="text-body font-semibold">Em construção</h2>
        <p className="text-body-sm max-w-130 text-text-muted">
          As telas de trabalho chegam com as próximas funcionalidades. Por enquanto, aqui
          você acessa a sua conta e as suas sessões.
        </p>
      </Panel>
    </main>
  )
}
