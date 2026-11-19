import {
  useMutation,
  useQuery,
  useQueryClient,
  type QueryClient,
} from "@tanstack/react-query"
import { useNavigate } from "react-router"

import * as service from "./iam.service"
import type { Role } from "./iam.service"

const ONE_HOUR = 60 * 60 * 1000

// Chaves: `["users"]` cobre a lista e o detalhe (`["users", id]`); `["sessions", userId]`, as sessões.
const keys = {
  me: ["me"] as const,
  users: ["users"] as const,
  user: (id: string) => ["users", id] as const,
  sessions: ["sessions"] as const,
  userSessions: (userId: string) => ["sessions", userId] as const,
}

/** Quem está logado. O 401 não repete (`shouldRetry` do cliente de query) e a tela decide pelo `code`. */
export function useCurrentUser() {
  return useQuery({
    queryKey: keys.me,
    queryFn: service.getCurrentUser,
    staleTime: ONE_HOUR,
    // A tela pública de login faz esta consulta sem sessão: o 401 é esperado, não é queda de sessão.
    meta: { handlesOwnSession: true },
  })
}

export function useUsers(input: { page: number; size: number }) {
  return useQuery({
    queryKey: [...keys.users, input],
    queryFn: () => service.listUsers(input),
  })
}

export function useUser(id: string) {
  return useQuery({ queryKey: keys.user(id), queryFn: () => service.getUser(id) })
}

export function useSessions(userId: string) {
  return useQuery({
    queryKey: keys.userSessions(userId),
    queryFn: () => service.listSessions(userId),
  })
}

const invalidateMe = (client: QueryClient) =>
  client.invalidateQueries({ queryKey: keys.me })
const invalidateUsers = (client: QueryClient) =>
  client.invalidateQueries({ queryKey: keys.users })
const invalidateSessions = (client: QueryClient) =>
  client.invalidateQueries({ queryKey: keys.sessions })

export function useLogin() {
  const client = useQueryClient()

  return useMutation({ mutationFn: service.login, onSuccess: () => invalidateMe(client) })
}

export function useAcceptInvitation() {
  const client = useQueryClient()

  return useMutation({
    mutationFn: service.acceptInvitation,
    onSuccess: () => invalidateMe(client),
  })
}

/**
 * Quem sai não deixa dado para o próximo usuário do aparelho: limpa o cache inteiro e vai para
 * o login. A navegação é daqui porque `clear()` não avisa a consulta `me` ainda montada, e o
 * guarda não notaria a saída.
 */
export function useLogout() {
  const client = useQueryClient()
  const navigate = useNavigate()

  return useMutation({
    mutationFn: service.logout,
    onSuccess: () => {
      client.clear()
      void navigate("/entrar", { replace: true })
    },
  })
}

export function useRequestPasswordReset() {
  return useMutation({ mutationFn: service.requestPasswordReset })
}

export function useResetPassword() {
  return useMutation({ mutationFn: service.resetPassword })
}

/** A troca zera `mustChangePassword` e encerra as outras sessões. */
export function useChangePassword() {
  const client = useQueryClient()

  return useMutation({
    mutationFn: service.changePassword,
    onSuccess: () => Promise.all([invalidateMe(client), invalidateSessions(client)]),
  })
}

export function useInviteUser() {
  const client = useQueryClient()

  return useMutation({
    mutationFn: service.inviteUser,
    onSuccess: () => invalidateUsers(client),
  })
}

export function useResendInvitation() {
  const client = useQueryClient()

  return useMutation({
    mutationFn: service.resendInvitation,
    onSuccess: () => invalidateUsers(client),
  })
}

export function useChangeUserRole() {
  const client = useQueryClient()

  return useMutation({
    mutationFn: ({ id, role }: { id: string; role: Role }) =>
      service.changeUserRole(id, role),
    onSuccess: () => invalidateUsers(client),
  })
}

/** Desativar revoga as sessões do usuário. */
export function useDeactivateUser() {
  const client = useQueryClient()

  return useMutation({
    mutationFn: service.deactivateUser,
    onSuccess: () => Promise.all([invalidateUsers(client), invalidateSessions(client)]),
  })
}

export function useReactivateUser() {
  const client = useQueryClient()

  return useMutation({
    mutationFn: service.reactivateUser,
    onSuccess: () => invalidateUsers(client),
  })
}

export function useRevokeSession() {
  const client = useQueryClient()

  return useMutation({
    mutationFn: service.revokeSession,
    onSuccess: () => Promise.all([invalidateSessions(client), invalidateUsers(client)]),
  })
}

export function useRevokeOtherSessions() {
  const client = useQueryClient()

  return useMutation({
    mutationFn: service.revokeOtherSessions,
    onSuccess: () => Promise.all([invalidateSessions(client), invalidateUsers(client)]),
  })
}

export function useRevokeAllSessions() {
  const client = useQueryClient()

  return useMutation({
    mutationFn: service.revokeAllSessions,
    onSuccess: () => Promise.all([invalidateSessions(client), invalidateUsers(client)]),
  })
}
