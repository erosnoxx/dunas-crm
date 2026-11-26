import { Link } from "react-router"

import { cn } from "@/shared/lib/cn"

type CatalogSection = "models" | "manufacturers" | "types"

const TABS: { section: CatalogSection; label: string; to: string }[] = [
  { section: "models", label: "Modelos", to: "/catalogo" },
  { section: "manufacturers", label: "Fabricantes", to: "/catalogo/fabricantes" },
  { section: "types", label: "Tipos", to: "/catalogo/tipos" },
]

/**
 * As três seções do catálogo como links de verdade. A partir do `md` são abas: a ativa leva o
 * traço de 3 px em `brand-text` sobre a linha `line`. No celular é um controle segmentado de
 * três partes iguais, e a ativa leva o fundo `chip-ok`.
 */
function CatalogTabs({ active }: { active: CatalogSection }) {
  return (
    <nav
      aria-label="Seções do catálogo"
      className="grid grid-cols-3 border border-line-strong md:flex md:gap-1 md:border-0 md:border-b md:border-line"
    >
      {TABS.map(({ section, label, to }) => (
        <Link
          key={section}
          to={to}
          aria-current={section === active ? "page" : undefined}
          className={cn(
            "flex h-11 items-center justify-center text-body-sm font-semibold text-text-muted aria-[current=page]:bg-chip-ok aria-[current=page]:text-text",
            "md:-mb-px md:h-auto md:justify-start md:border-b-3 md:border-transparent md:px-4.5 md:py-3 md:aria-[current=page]:border-brand-text md:aria-[current=page]:bg-transparent md:aria-[current=page]:text-brand-text",
          )}
        >
          {label}
        </Link>
      ))}
    </nav>
  )
}

export { CatalogTabs }
