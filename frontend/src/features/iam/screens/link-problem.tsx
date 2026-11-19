import * as React from "react"
import { ClockIcon } from "lucide-react"
import { Link } from "react-router"

import { Button } from "@/shared/ui/button"
import { Panel } from "@/shared/ui/panel"

import { AuthHeader } from "../components/auth-shell"

const COPY = {
  invitation: {
    expired: {
      title: "Este convite expirou",
      text: "Por segurança, o link do convite deixa de valer depois de um tempo. Peça ao administrador para enviar um novo convite.",
    },
    used: {
      title: "Este convite já foi usado ou foi substituído",
      text: "Peça ao administrador para enviar um novo convite.",
    },
    action: { label: "Ir para o login", to: "/entrar" },
  },
  // Copy aprovada em telas-complementares.md: um texto só para os dois títulos.
  reset: {
    expired: {
      title: "Este link expirou",
      text: "Por segurança, o link deixa de valer depois de um tempo. Peça um novo na tela de login.",
    },
    used: {
      title: "Este link já foi usado ou foi substituído",
      text: "Por segurança, o link deixa de valer depois de um tempo. Peça um novo na tela de login.",
    },
    action: { label: "Pedir novo link", to: "/esqueci-senha" },
  },
} as const

/**
 * Link que não vale mais (mockup "Convite · link expirado"; `used` reaproveita o desenho com o
 * texto de `link_used`, e `source="reset"` o texto de "link" da redefinição de senha). No
 * celular o painel `cut-lg` fica acima do botão; a partir de `md` ele é centralizado, como o
 * painel do `AuthShell`.
 */
function LinkProblem({
  kind,
  source = "invitation",
}: {
  kind: "expired" | "used"
  source?: keyof typeof COPY
}) {
  const titleId = React.useId()
  const copy = COPY[source]
  const { title, text } = copy[kind]
  const { action } = copy

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
            render={<Link to={action.to} />}
            role="link"
            className="w-full"
          >
            {action.label}
          </Button>
        </div>
      </main>
    </div>
  )
}

export { LinkProblem }
