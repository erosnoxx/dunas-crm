import type { ReactNode } from "react"
import { UsersIcon } from "lucide-react"
import { NavLink, useLocation } from "react-router"

import { CHANGE_PASSWORD_PATH } from "@/features/iam/guards"
import { useCurrentUser, useLogout } from "@/features/iam/iam.hooks"
import { LABELS } from "@/features/iam/iam.messages"
import type { Role } from "@/features/iam/iam.service"
import { Logo } from "@/shared/ui/logo"

import { AccountMenu, UserAvatar } from "./account-menu"

type NavItem = { to: string; label: string; roles: Role[] }

/** Itens de trabalho da barra lateral. Só o Administrador tem item hoje (ADR-5: só ele gerencia usuários). */
const NAV_ITEMS: NavItem[] = [
  { to: "/usuarios", label: "Usuários", roles: ["ADMINISTRATOR"] },
]

/**
 * Título da barra superior do celular, por rota exata; a troca de senha tem cabeçalho próprio.
 * Contrato: a página é dona do h1 e o esconde com `max-md:sr-only` no celular; rota aninhada
 * precisa de entrada aqui ou de casamento por prefixo.
 */
const TITLES: Record<string, string> = {
  "/": "Início",
  "/usuarios": "Usuários",
  "/conta/sessoes": "Minhas sessões",
}

const ACCOUNT_LINK =
  "flex min-h-11 items-center px-2 text-body-sm text-text-muted hover:text-text aria-[current=page]:text-text"

/**
 * Casca das rotas autenticadas. A partir de `md` é a barra lateral de 232 px (mockups "Usuários ·
 * web" e "Navegação, web"); no celular, a barra superior de 64 px com o avatar que abre o menu da
 * conta (mockup "Usuários · celular"). A troca de senha obrigatória não ganha casca: é tela cheia
 * só com "Sair". A voluntária fica na casca, mas no celular sem a barra superior, porque o
 * cabeçalho próprio dela (com "Voltar") já ocupa o topo.
 */
function AppShell({ children }: { children: ReactNode }) {
  const { data: me } = useCurrentUser()
  const { pathname } = useLocation()
  const logout = useLogout()

  if (!me || me.mustChangePassword) return children

  const topBar = pathname !== CHANGE_PASSWORD_PATH

  return (
    <div className="flex min-h-dvh flex-col md:flex-row">
      {topBar && (
        <header className="flex h-16 shrink-0 items-center gap-3 border-b border-line bg-nav px-4 md:hidden">
          <Logo size={36} alt="" />
          <p className="text-title flex-1">{TITLES[pathname]}</p>
          <AccountMenu user={me} />
        </header>
      )}

      <nav
        aria-label="Principal"
        className="hidden shrink-0 flex-col gap-8 bg-nav px-4 py-6 md:sticky md:top-0 md:flex md:h-dvh md:w-58"
      >
        <div className="flex items-center gap-3 px-2">
          <Logo size={40} alt="" />
          <span className="font-display text-xl font-semibold tracking-[0.04em]">
            DUNAS
          </span>
        </div>

        <div className="flex flex-col gap-1.5">
          {NAV_ITEMS.filter((item) => item.roles.includes(me.role)).map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              className="group flex h-11 text-body-sm font-semibold"
            >
              {/* O corte fica no miolo: `clip-path` no link cortaria o anel de foco. */}
              <span className="cut-sm flex flex-1 items-center gap-3 px-3.5 group-aria-[current=page]:bg-chip-ok">
                <UsersIcon aria-hidden="true" className="size-5" />
                {item.label}
              </span>
            </NavLink>
          ))}
        </div>

        <div className="mt-auto flex flex-col gap-2">
          <div className="flex items-center gap-3 px-2">
            <UserAvatar name={me.name} className="size-10 text-lg" />
            <div className="flex flex-col leading-tight">
              <span className="text-body-sm font-semibold">{me.name}</span>
              <span className="text-label font-normal text-text-muted">
                {LABELS.role[me.role]}
              </span>
            </div>
          </div>
          <NavLink to="/conta/sessoes" className={ACCOUNT_LINK}>
            Minhas sessões
          </NavLink>
          <NavLink to={CHANGE_PASSWORD_PATH} className={ACCOUNT_LINK}>
            Trocar senha
          </NavLink>
          <button
            type="button"
            disabled={logout.isPending}
            onClick={() => logout.mutate()}
            className={`${ACCOUNT_LINK} text-start`}
          >
            Sair
          </button>
        </div>
      </nav>

      <div className="min-w-0 flex-1">{children}</div>
    </div>
  )
}

export { AppShell }
