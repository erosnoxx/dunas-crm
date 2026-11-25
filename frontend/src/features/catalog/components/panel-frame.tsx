import * as React from "react"

import { cn } from "@/shared/lib/cn"
import { MD_UP, useMediaQuery } from "@/shared/lib/use-media-query"
import { Button } from "@/shared/ui/button"
import { MobileHeader } from "@/shared/ui/mobile-header"

// Botões da web (mockups 7 e 9): 44 px e 14 px peso 600; o do celular é o do rodapé fixo, 48 px e
// 16 px. `md:font-semibold` porque o `md:text-body-sm` traz o peso 400 do token.
const WEB_BUTTON = "self-start md:text-body-sm md:font-semibold"

/**
 * A moldura do painel de uma lista com painel ao lado (fabricantes e tipos). Na web: o painel
 * `panel` do mockup, com o título em h2. No celular (derivado do mockup 11): o cabeçalho próprio
 * com "Voltar" para `to` e a ação principal no rodapé fixo, com a folga dela no fim do conteúdo (a
 * casca não põe as barras nessas rotas).
 */
function PanelFrame({
  to,
  title,
  mobileAction,
  children,
}: {
  to: string
  title: string
  mobileAction?: React.ReactNode
  children: React.ReactNode
}) {
  const isDesktop = useMediaQuery(MD_UP)

  if (isDesktop)
    return (
      <section className="flex flex-col gap-4 border border-line bg-panel p-6">
        <h2 className="font-display text-xl leading-6 font-semibold">{title}</h2>
        {children}
      </section>
    )

  return (
    <>
      <MobileHeader to={to} title={title} />
      <div
        className={cn("flex flex-col gap-4.5 px-4 pt-5", mobileAction ? "pb-24" : "pb-5")}
      >
        {children}
      </div>
      {mobileAction && (
        <div className="fixed inset-x-0 bottom-0 border-t border-line bg-nav px-4 py-3">
          {mobileAction}
        </div>
      )}
    </>
  )
}

/** O botão de enviar o formulário `formId`: no painel da web e no rodapé fixo do celular. */
function SaveButton({
  formId,
  label,
  loading,
}: {
  formId: string
  label: string
  loading: boolean
}) {
  const isDesktop = useMediaQuery(MD_UP)

  return (
    <Button
      type="submit"
      form={formId}
      size={isDesktop ? "md" : "lg"}
      loading={loading}
      className={isDesktop ? WEB_BUTTON : "min-h-12 w-full"}
    >
      {label}
    </Button>
  )
}

export { PanelFrame, SaveButton, WEB_BUTTON }
