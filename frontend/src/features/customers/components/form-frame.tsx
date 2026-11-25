import { ChevronLeftIcon } from "lucide-react"
import * as React from "react"
import { Link } from "react-router"

import type { ApiError } from "@/shared/api/api-error"
import { MD_UP, useMediaQuery } from "@/shared/lib/use-media-query"
import { MobileHeader } from "@/shared/ui/mobile-header"

// Botões da web (telas 7, 14 e 17): 44 px e 14 px peso 600; o do celular é o do rodapé fixo, 48 px
// e 16 px (telas 8, 15 e 18). `md:font-semibold` porque o `md:text-body-sm` traz o peso 400.
const WEB_BUTTON = "md:text-body-sm md:font-semibold"

type Failure = Partial<ApiError> | null
type Back = { to: string; label: string }

/**
 * A moldura dos formulários de cliente, local e equipamento. Na web: o link de volta e o título em
 * `display-lg` (tela 7, linhas 25 a 27; tela 14, linhas 25 a 27; tela 17, linhas 25 a 27). No
 * celular: o cabeçalho próprio (telas 8, 15 e 18, linhas 17 a 19) e a ação no rodapé fixo (tela 15,
 * linhas 32 e 33), com a folga dela no fim do conteúdo. Sem título (carregando, erro) só há o voltar.
 */
function Frame({
  back,
  title,
  mobileAction,
  children,
}: {
  back: Back
  title?: string
  mobileAction?: React.ReactNode
  children: React.ReactNode
}) {
  const isDesktop = useMediaQuery(MD_UP)

  if (isDesktop)
    return (
      <main className="flex flex-col gap-5 p-8">
        <div>
          <Link
            to={back.to}
            className="inline-flex min-h-11 items-center gap-1.5 text-body-sm font-semibold text-text-muted"
          >
            <ChevronLeftIcon aria-hidden="true" strokeWidth={2.5} className="size-4.5" />
            {back.label}
          </Link>
          {title && <h1 className="text-display-lg">{title}</h1>}
        </div>
        {children}
      </main>
    )

  return (
    <>
      <MobileHeader to={back.to} title={title} />
      <main
        className={
          mobileAction
            ? "flex flex-col gap-4 px-4 pt-5 pb-24"
            : "flex flex-col gap-4 p-4 pt-5"
        }
      >
        {children}
      </main>
      {mobileAction && (
        <div className="fixed inset-x-0 bottom-0 border-t border-line bg-nav px-4 py-3">
          {mobileAction}
        </div>
      )}
    </>
  )
}

export { Frame, WEB_BUTTON, type Back, type Failure }
