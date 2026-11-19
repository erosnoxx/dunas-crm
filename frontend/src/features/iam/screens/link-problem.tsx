import * as React from "react"
import { ClockIcon } from "lucide-react"
import { Link } from "react-router"

import { Button } from "@/shared/ui/button"
import { Panel } from "@/shared/ui/panel"

import { AuthHeader } from "../components/auth-shell"

const COPY = {
  expired: {
    title: "Este convite expirou",
    text: "Por segurança, o link do convite deixa de valer depois de um tempo. Peça ao administrador para enviar um novo convite.",
  },
  used: {
    title: "Este convite já foi usado ou foi substituído",
    text: "Peça ao administrador para enviar um novo convite.",
  },
} as const

/**
 * Link do convite que não vale mais (mockup "Convite · link expirado"; `used` reaproveita o
 * desenho com o texto de `link_used`). No celular o painel `cut-lg` fica acima do botão; a partir
 * de `md` ele é centralizado, como o painel do `AuthShell`.
 */
function LinkProblem({ kind }: { kind: keyof typeof COPY }) {
  const titleId = React.useId()
  const { title, text } = COPY[kind]

  return (
    <div className="flex min-h-dvh flex-col gap-8 px-6 pt-8 pb-6 md:gap-0 md:p-0">
      <AuthHeader />
      <main className="flex flex-1 flex-col md:items-center md:justify-center md:px-4 md:pt-8 md:pb-16">
        <div className="flex w-full max-w-110 flex-col gap-6">
          <Panel
            aria-labelledby={titleId}
            className="flex flex-col gap-4 px-6 py-7 md:px-9 md:py-10"
          >
            <ClockIcon aria-hidden="true" className="size-8 text-highlight" />
            <h1 id={titleId} className="text-title">
              {title}
            </h1>
            <p className="text-body">{text}</p>
          </Panel>
          <Button
            variant="outline"
            size="lg"
            nativeButton={false}
            render={<Link to="/entrar" />}
            role="link"
            className="w-full"
          >
            Ir para o login
          </Button>
        </div>
      </main>
    </div>
  )
}

export { LinkProblem }
