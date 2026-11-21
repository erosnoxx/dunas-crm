import { ChevronLeftIcon } from "lucide-react"
import { Link } from "react-router"

import { cn } from "@/shared/lib/cn"

type MobileHeaderProps = {
  /** Para onde o "Voltar" leva. */
  to: string
  /** O h1 da tela; sem ele (carregando, erro) só há o "Voltar". */
  title?: string
  className?: string
  backClassName?: string
  titleClassName?: string
}

/**
 * Cabeçalho próprio das telas do celular (64 px, com "Voltar" e o título no h1): a casca não põe
 * a barra superior nessas rotas. As telas que também existem na web passam as classes `md:` que
 * o transformam no título da página.
 */
function MobileHeader({
  to,
  title,
  className,
  backClassName,
  titleClassName,
}: MobileHeaderProps) {
  return (
    <header
      className={cn(
        "flex h-16 items-center gap-1 border-b border-line bg-nav px-2",
        className,
      )}
    >
      <Link
        to={to}
        aria-label="Voltar"
        className={cn("flex size-12 items-center justify-center", backClassName)}
      >
        <ChevronLeftIcon aria-hidden="true" className="size-6" />
      </Link>
      {title && <h1 className={cn("text-title", titleClassName)}>{title}</h1>}
    </header>
  )
}

export { MobileHeader }
