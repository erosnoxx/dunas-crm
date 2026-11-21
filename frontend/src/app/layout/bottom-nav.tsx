import type { LucideIcon } from "lucide-react"
import { NavLink } from "react-router"

import type { Role } from "@/features/iam/iam.service"

type NavItem = { to: string; label: string; icon: LucideIcon; roles: Role[] }

/**
 * Barra inferior do celular (mockups "Modelos · celular" e "Modelos · celular, Técnico"): 64 px,
 * um item por link que o papel vê, repartidos em partes iguais, com ícone sobre o rótulo. O
 * atual leva `brand-text`. É `sticky bottom-0` no fim da coluna da casca: fica no pé da tela
 * sem cobrir o conteúdo, que não precisa de folga. O rótulo difere do da barra lateral para que
 * as duas não se confundam como a mesma região.
 */
function BottomNav({ items }: { items: NavItem[] }) {
  return (
    <nav
      aria-label="Principal no celular"
      className="sticky bottom-0 z-10 grid h-16 shrink-0 auto-cols-fr grid-flow-col border-t border-line bg-nav md:hidden"
    >
      {items.map(({ to, label, icon: Icon }) => (
        <NavLink
          key={to}
          to={to}
          className="text-caption flex flex-col items-center justify-center gap-0.5 font-semibold text-text-muted aria-[current=page]:text-brand-text"
        >
          <Icon aria-hidden="true" className="size-5.5" />
          {label}
        </NavLink>
      ))}
    </nav>
  )
}

export { BottomNav }
export type { NavItem }
