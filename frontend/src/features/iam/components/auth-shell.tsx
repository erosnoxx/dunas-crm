import * as React from "react"

import { cn } from "@/shared/lib/cn"
import { Logo } from "@/shared/ui/logo"
import { Panel } from "@/shared/ui/panel"

type AuthShellProps = {
  title: string
  description: string
  children: React.ReactNode
  /** Nota sob o painel (no celular, no pé da tela), como "O acesso é por convite..." do Entrar. */
  footer?: React.ReactNode
  /**
   * Espaços do celular do mockup "Convite · definir senha": 24 do cabeçalho ao conteúdo e 20 do
   * título ao formulário (o Entrar usa 32 e 24).
   */
  compact?: boolean
}

/** Logo e nome no topo das telas de acesso. */
function AuthHeader() {
  return (
    <header className="flex items-center gap-3 md:px-8 md:py-6">
      <Logo size={40} alt="" />
      <span className="font-display text-xl font-semibold tracking-[0.04em]">DUNAS</span>
    </header>
  )
}

/**
 * Casca das telas de acesso. No celular é a tela cheia; a partir de `md` o conteúdo vai num
 * painel `cut-lg` de 440 px, centralizado (mockup "Entrar · web"). Sobre o painel os campos
 * usam `ground`, como no mockup.
 */
function AuthShell({
  title,
  description,
  children,
  footer,
  compact = false,
}: AuthShellProps) {
  const titleId = React.useId()

  return (
    <div
      className={cn(
        "flex min-h-dvh flex-col px-6 pt-8 pb-6 md:gap-0 md:p-0",
        compact ? "gap-6" : "gap-8",
      )}
    >
      <AuthHeader />
      <main className="flex flex-1 flex-col md:items-center md:justify-center md:px-4 md:pt-8 md:pb-16">
        <Panel
          aria-labelledby={titleId}
          className={cn(
            "flex w-full max-w-110 flex-col max-md:bg-transparent max-md:[clip-path:none] md:px-9 md:py-10 md:**:data-[slot=input]:bg-ground",
            compact ? "gap-5 md:gap-6" : "gap-6",
          )}
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
      {footer && (
        <div className="text-label font-normal text-text-muted md:px-8 md:pb-6 md:text-center">
          {footer}
        </div>
      )}
    </div>
  )
}

export { AuthHeader, AuthShell }
