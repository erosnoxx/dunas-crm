import { CircleCheckIcon, CircleIcon } from "lucide-react"

import { cn } from "@/shared/lib/cn"

import { checkPassword } from "../password-rules"

/**
 * As regras da senha, na ordem do mockup. A situação se lê pela forma (círculo cheio com visto
 * ou vazado), além da cor do texto e do texto só para leitor de tela.
 */
function PasswordChecklist({ password }: { password: string }) {
  return (
    <ul aria-label="Regras da senha" className="flex flex-col gap-2">
      {checkPassword(password).map((rule) => (
        <li
          key={rule.id}
          className={cn(
            "flex items-center gap-2.5 text-body-sm",
            rule.met ? "text-text" : "text-text-muted",
          )}
        >
          {rule.met ? (
            <CircleCheckIcon
              aria-hidden="true"
              data-shape="filled"
              className="size-5 shrink-0 fill-chip-ok stroke-text"
            />
          ) : (
            <CircleIcon
              aria-hidden="true"
              data-shape="ring"
              className="size-5 shrink-0 stroke-text-muted"
            />
          )}
          <span>
            {rule.label}
            <span className="sr-only">
              {rule.met ? ", atendida" : ", ainda não atendida"}
            </span>
          </span>
        </li>
      ))}
    </ul>
  )
}

export { PasswordChecklist }
