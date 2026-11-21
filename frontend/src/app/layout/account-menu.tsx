import * as React from "react"
import { Dialog } from "@base-ui/react/dialog"
import { ClockIcon, KeyRoundIcon, LogOutIcon, XIcon } from "lucide-react"
import { Link } from "react-router"

import { useLogout } from "@/features/iam/iam.hooks"
import { LABELS } from "@/features/iam/iam.messages"
import type { CurrentUser } from "@/features/iam/iam.service"
import { cn } from "@/shared/lib/cn"
import { MD_UP, useMediaQuery } from "@/shared/lib/use-media-query"

/** Avatar com a inicial do nome (mockups "Usuários · web" e "Menu da conta, celular"). */
function UserAvatar({ name, className }: { name: string; className?: string }) {
  return (
    <span
      aria-hidden="true"
      className={cn(
        "cut-sm flex shrink-0 items-center justify-center bg-brand font-display font-bold text-on-brand",
        className,
      )}
    >
      {name.trim().charAt(0).toLocaleUpperCase("pt-BR")}
    </span>
  )
}

const ITEM =
  "flex min-h-13 w-full items-center gap-3.5 border-t border-line px-1 text-body font-medium"

/**
 * Menu da conta do celular (mockup "Menu da conta, celular"): o avatar da barra superior abre
 * uma folha modal com Minhas sessões, Trocar senha e Sair. Quem cuida do Esc, do foco preso e do
 * retorno do foco ao avatar é o diálogo do Base UI.
 */
function AccountMenu({ user }: { user: CurrentUser }) {
  const [open, setOpen] = React.useState(false)
  const logout = useLogout()
  const close = () => setOpen(false)

  // A folha some em `md` (`md:hidden`), mas o diálogo seguiria modal: fecha ao cruzar o breakpoint.
  const isDesktop = useMediaQuery(MD_UP)

  if (isDesktop && open) setOpen(false)

  return (
    <Dialog.Root open={open} onOpenChange={setOpen}>
      <Dialog.Trigger
        aria-label={`Minha conta, ${user.name}`}
        className="size-11 shrink-0"
      >
        <UserAvatar name={user.name} className="size-full text-lg" />
      </Dialog.Trigger>
      <Dialog.Portal>
        <Dialog.Backdrop className="fixed inset-0 bg-ground/80 md:hidden" />
        <Dialog.Popup className="cut-lg fixed inset-x-0 bottom-0 flex flex-col gap-2 bg-panel px-5 pt-5 pb-7 md:hidden">
          <Dialog.Title className="sr-only">Minha conta</Dialog.Title>
          <div className="flex items-center gap-3 pb-4">
            <UserAvatar name={user.name} className="size-12 text-xl" />
            <div className="flex flex-1 flex-col leading-tight">
              <span className="text-body font-semibold">{user.name}</span>
              <span className="text-body-sm text-text-muted">
                {LABELS.role[user.role]}
              </span>
            </div>
            <Dialog.Close
              aria-label="Fechar"
              className="flex size-11 items-center justify-center"
            >
              <XIcon aria-hidden="true" className="size-6" />
            </Dialog.Close>
          </div>
          <Link to="/conta/sessoes" onClick={close} className={ITEM}>
            <ClockIcon aria-hidden="true" className="size-5 text-text-muted" />
            Minhas sessões
          </Link>
          <Link to="/conta/trocar-senha" onClick={close} className={ITEM}>
            <KeyRoundIcon aria-hidden="true" className="size-5 text-text-muted" />
            Trocar senha
          </Link>
          <button
            type="button"
            disabled={logout.isPending}
            onClick={() => logout.mutate()}
            className={cn(ITEM, "text-start text-danger-text")}
          >
            <LogOutIcon aria-hidden="true" className="size-5" />
            Sair
          </button>
        </Dialog.Popup>
      </Dialog.Portal>
    </Dialog.Root>
  )
}

export { AccountMenu, UserAvatar }
