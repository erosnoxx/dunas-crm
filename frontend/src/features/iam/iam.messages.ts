import { UNKNOWN_ERROR_MESSAGE, type ApiError } from "@/shared/api/api-error"

import type { Role, UserStatus } from "./iam.service"

/**
 * Mensagens da tabela de erros de docs/system-design/01-iam/usecases.md, por `code`.
 * `invalid_password` fica de fora de propósito: a mensagem depende do critério que falta, e
 * quem a conhece é a resposta da API (o `fallback`).
 */
const MESSAGES: Record<string, string> = {
  invalid_credentials: "e-mail ou senha incorretos",
  session_invalid: "sua sessão não vale mais; entre de novo",
  rate_limited: "muitas tentativas; tente de novo em alguns minutos",
  password_change_required: "você precisa trocar a senha antes de continuar",
  forbidden: "você não tem permissão para isto",
  link_expired: "este link expirou",
  link_used: "este link já foi usado ou foi substituído",
  email_already_registered: "já existe um usuário com este e-mail",
  user_not_found: "usuário não encontrado",
  session_not_found: "sessão não encontrada",
  user_not_invited: "este usuário não está com convite pendente",
  user_not_active: "este usuário não está ativo",
  user_deactivated: "este usuário já está desativado",
  user_not_deactivated: "este usuário não está desativado",
  cannot_change_own_role: "você não pode mudar o seu próprio papel",
  cannot_deactivate_self: "você não pode desativar a sua própria conta",
  wrong_current_password: "a senha atual está incorreta",
}

export function messageFor(code: string | undefined, fallback: string): string {
  return (code !== undefined && MESSAGES[code]) || fallback
}

export const LABELS: { role: Record<Role, string>; status: Record<UserStatus, string> } =
  {
    role: {
      ADMINISTRATOR: "Administrador",
      COMMERCIAL: "Comercial",
      TECHNICIAN: "Técnico",
    },
    status: { ACTIVE: "Ativo", INVITED: "Convite pendente", DEACTIVATED: "Desativado" },
  }

/** Os papéis na ordem em que as telas os listam. */
export const ROLES = Object.keys(LABELS.role) as Role[]

/** As mensagens da API vêm em minúscula; no aviso a frase começa com maiúscula. */
export const sentence = (text: string) => text.charAt(0).toUpperCase() + text.slice(1)

/** O nome na tela: o do próprio usuário leva "(você)". */
export const displayName = (name: string, isMe: boolean) =>
  isMe ? `${name} (você)` : name

/** Tom do chip de cada situação (círculo cheio, anel ou cortado). */
export const STATUS_TONE: Record<UserStatus, "ok" | "pending" | "off"> = {
  ACTIVE: "ok",
  INVITED: "pending",
  DEACTIVATED: "off",
}

/** A mensagem de um erro da API para o aviso: pelo `code`, senão a da própria API, senão a genérica. */
export const errorMessage = (error: Partial<ApiError>) =>
  sentence(messageFor(error.code, error.message || UNKNOWN_ERROR_MESSAGE))
