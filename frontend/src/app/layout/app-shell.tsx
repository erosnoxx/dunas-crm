import type { ReactNode } from "react"
import { PackageIcon, UsersIcon } from "lucide-react"
import { NavLink, useLocation } from "react-router"

import { CHANGE_PASSWORD_PATH } from "@/features/iam/guards"
import { useCurrentUser, useLogout } from "@/features/iam/iam.hooks"
import { LABELS } from "@/features/iam/iam.messages"
import { Logo } from "@/shared/ui/logo"

import { AccountMenu, UserAvatar } from "./account-menu"
import { BottomNav, type NavItem } from "./bottom-nav"

/**
 * Itens de trabalho, na barra lateral e na inferior. Catálogo é de todos os papéis (o Técnico só
 * consulta, ADR-6); Usuários é só do Administrador (ADR-5).
 */
const NAV_ITEMS: NavItem[] = [
  {
    to: "/catalogo",
    label: "Catálogo",
    icon: PackageIcon,
    roles: ["ADMINISTRATOR", "COMMERCIAL", "TECHNICIAN"],
  },
  { to: "/usuarios", label: "Usuários", icon: UsersIcon, roles: ["ADMINISTRATOR"] },
]

/**
 * Título da barra superior do celular, por rota exata ou pelo item do menu sob o qual a rota está
 * (`/catalogo`, `/catalogo/fabricantes`...). Contrato: a página é dona do h1 e o esconde com
 * `max-md:sr-only` no celular; rota aninhada precisa de entrada aqui ou de item do menu. As rotas
 * de `OWN_HEADER` não passam por aqui: a página desenha o cabeçalho.
 */
const TITLES: Record<string, string> = {
  "/": "Início",
  "/usuarios": "Usuários",
}

/** Rotas com cabeçalho próprio no celular (com "Voltar"): a casca não põe a barra superior nem a inferior. */
const OWN_HEADER = [CHANGE_PASSWORD_PATH, "/conta/sessoes"]

/**
 * O mesmo, por prefixo, com a barra final: `/usuarios/:id` e `/usuarios/convidar`, o modelo
 * (`/catalogo/modelos/novo` e `:id`) e os formulários de fabricante e tipo. A barra final deixa
 * as listas (`/catalogo/fabricantes`, `/catalogo/tipos`) com as barras da casca.
 */
const OWN_HEADER_PREFIXES = [
  "/usuarios/",
  "/catalogo/modelos/",
  "/catalogo/fabricantes/",
  "/catalogo/tipos/",
]

const titleOf = (pathname: string) =>
  TITLES[pathname] ??
  NAV_ITEMS.find(({ to }) => pathname === to || pathname.startsWith(`${to}/`))?.label

const ACCOUNT_LINK =
  "flex min-h-11 items-center px-2 text-body-sm text-text-muted hover:text-text aria-[current=page]:font-semibold aria-[current=page]:text-text"

/**
 * Casca das rotas autenticadas. A partir de `md` é a barra lateral de 232 px (mockups "Usuários ·
 * web" e "Navegação, web"); no celular, a barra superior de 64 px com o avatar que abre o menu da
 * conta (mockup "Usuários · celular"). A troca de senha obrigatória não ganha casca: é tela cheia
 * só com "Sair". A voluntária e Minhas sessões ficam na casca, mas no celular sem a barra
 * superior, porque o cabeçalho próprio delas (com "Voltar") já ocupa o topo.
 */
function AppShell({ children }: { children: ReactNode }) {
  const { data: me } = useCurrentUser()
  const { pathname } = useLocation()
  const logout = useLogout()

  if (!me || me.mustChangePassword) return children

  const topBar =
    !OWN_HEADER.includes(pathname) &&
    !OWN_HEADER_PREFIXES.some((prefix) => pathname.startsWith(prefix))
  const items = NAV_ITEMS.filter((item) => item.roles.includes(me.role))

  return (
    <div className="flex min-h-dvh flex-col md:flex-row">
      {topBar && (
        <header className="flex h-16 shrink-0 items-center gap-3 border-b border-line bg-nav px-4 md:hidden">
          <Logo size={36} alt="" />
          <p className="text-title flex-1">{titleOf(pathname)}</p>
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
          {items.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              className="group flex h-11 text-body-sm font-semibold"
            >
              {/* O corte fica no miolo: `clip-path` no link cortaria o anel de foco. */}
              <span className="cut-sm flex flex-1 items-center gap-3 px-3.5 text-text-muted group-aria-[current=page]:bg-chip-ok group-aria-[current=page]:text-text">
                <item.icon aria-hidden="true" className="size-5" />
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

      {topBar && <BottomNav items={items} />}
    </div>
  )
}

export { AppShell }
