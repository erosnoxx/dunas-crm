import * as React from "react"

import { Logo } from "@/shared/ui/logo"
import { Panel } from "@/shared/ui/panel"

type AuthShellProps = {
  title: string
  description: string
  children: React.ReactNode
}

/**
 * Casca das telas de acesso. No celular é a tela cheia; a partir de `md` o conteúdo vai num
 * painel `cut-lg` de 440 px, centralizado (mockup "Entrar · web"). Sobre o painel os campos
 * usam `ground`, como no mockup.
 */
function AuthShell({ title, description, children }: AuthShellProps) {
  const titleId = React.useId()

  return (
    <div className="flex min-h-dvh flex-col gap-8 px-6 pt-8 pb-6 md:gap-0 md:p-0">
      <header className="flex items-center gap-3 md:px-8 md:py-6">
        <Logo size={40} />
        <span className="font-display text-xl font-semibold tracking-[0.04em]">
          DUNAS
        </span>
      </header>
      <main className="flex flex-1 flex-col md:items-center md:justify-center md:px-4 md:pt-8 md:pb-16">
        <Panel
          aria-labelledby={titleId}
          className="flex w-full max-w-110 flex-col gap-6 max-md:bg-transparent max-md:[clip-path:none] md:px-9 md:py-10 md:**:data-[slot=input]:bg-ground"
        >
          <div className="flex flex-col gap-2">
            <h1 id={titleId} className="text-display-lg">
              {title}
            </h1>
            <p className="text-body-sm text-text-muted">{description}</p>
          </div>
          {children}
        </Panel>
      </main>
    </div>
  )
}

export { AuthShell }
