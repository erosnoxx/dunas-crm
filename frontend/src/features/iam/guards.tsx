import type { ReactNode } from "react"
import { Navigate, useLocation } from "react-router"

import type { ApiError } from "@/shared/api/api-error"

import { useCurrentUser } from "./iam.hooks"
import type { Role } from "./iam.service"

const LOGIN_PATH = "/entrar"
const CHANGE_PASSWORD_PATH = "/conta/trocar-senha"

/** Sem `me` válido (401) vai para o login. Carregando ou com falha de rede, não mostra nem decide nada. */
export function RequireAuth({ children }: { children: ReactNode }) {
  const { data: me, error } = useCurrentUser()

  if (me) return children
  if ((error as Partial<ApiError> | null)?.status === 401)
    return <Navigate to={LOGIN_PATH} replace />

  return null
}

/** Dentro de `RequireAuth`: papel diferente do exigido volta para `/`. */
export function RequireRole({ role, children }: { role: Role; children: ReactNode }) {
  const { data: me } = useCurrentUser()

  if (!me) return null
  if (me.role !== role) return <Navigate to="/" replace />

  return children
}

/** Dentro de `RequireAuth`: sessão restrita (troca obrigatória) só abre a troca de senha. */
export function RequireSettledPassword({ children }: { children: ReactNode }) {
  const { data: me } = useCurrentUser()
  const { pathname } = useLocation()

  if (!me) return null
  if (me.mustChangePassword && pathname !== CHANGE_PASSWORD_PATH)
    return <Navigate to={CHANGE_PASSWORD_PATH} replace />

  return children
}

/**
 * Rota pública (login e afins): mostra a tela mesmo sem sessão (o 401 do `me` é o esperado) e
 * só manda para `/` quem já entrou.
 */
export function RedirectIfAuthenticated({ children }: { children: ReactNode }) {
  const { data: me } = useCurrentUser()

  if (me) return <Navigate to="/" replace />

  return children
}
