import { ensureCsrfCookie } from "@/shared/api/csrf"
import { httpClient } from "@/shared/api/http-client"
import type { Page } from "@/shared/api/page"

export type Role = "ADMINISTRATOR" | "COMMERCIAL" | "TECHNICIAN"
export type UserStatus = "INVITED" | "ACTIVE" | "DEACTIVATED"

/** O token da sessão viaja no cookie httpOnly; o corpo só diz até quando vale. */
export type SessionView = { expiresAt: string; mustChangePassword: boolean }

export type CurrentUser = {
  id: string
  name: string
  email: string
  role: Role
  mustChangePassword: boolean
}

export type UserSummary = {
  id: string
  name: string
  email: string
  role: Role
  status: UserStatus
  lastUsedAt: string | null
}

export type SessionSummary = {
  id: string
  device: string
  startedAt: string
  lastUsedAt: string
  current: boolean
}

export type UserDetail = { user: UserSummary; sessions: SessionSummary[] }

/** Só o que a tela precisa: nunca repassa um `token` que o corpo traga por engano. */
function toSessionView(data: SessionView): SessionView {
  return { expiresAt: data.expiresAt, mustChangePassword: data.mustChangePassword }
}

export async function login(input: {
  email: string
  password: string
}): Promise<SessionView> {
  await ensureCsrfCookie()
  const { data } = await httpClient.post<SessionView>("/auth/login", input)

  return toSessionView(data)
}

export async function logout(): Promise<void> {
  await httpClient.post("/auth/logout")
}

export async function acceptInvitation(input: {
  token: string
  password: string
}): Promise<SessionView> {
  await ensureCsrfCookie()
  const { data } = await httpClient.post<SessionView>("/auth/invitation/accept", input)

  return toSessionView(data)
}

export async function requestPasswordReset(input: { email: string }): Promise<void> {
  await ensureCsrfCookie()
  await httpClient.post("/auth/password/forgot", input)
}

export async function resetPassword(input: {
  token: string
  password: string
}): Promise<void> {
  await ensureCsrfCookie()
  await httpClient.post("/auth/password/reset", input)
}

export async function changePassword(input: {
  currentPassword: string
  newPassword: string
}): Promise<void> {
  await httpClient.put("/auth/password", input)
}

export async function getCurrentUser(): Promise<CurrentUser> {
  const { data } = await httpClient.get<CurrentUser>("/auth/me")

  return data
}

export async function listUsers(input: {
  page: number
  size: number
}): Promise<Page<UserSummary>> {
  const { data } = await httpClient.get<Page<UserSummary>>("/users", { params: input })

  return data
}

export async function getUser(id: string): Promise<UserDetail> {
  const { data } = await httpClient.get<UserDetail>(`/users/${id}`)

  return data
}

export async function inviteUser(input: {
  name: string
  email: string
  role: Role
}): Promise<{ id: string }> {
  const { data } = await httpClient.post<{ id: string }>("/users", input)

  return data
}

export async function resendInvitation(id: string): Promise<void> {
  await httpClient.post(`/users/${id}/invitation`)
}

export async function changeUserRole(id: string, role: Role): Promise<void> {
  await httpClient.put(`/users/${id}/role`, { role })
}

export async function deactivateUser(id: string): Promise<void> {
  await httpClient.post(`/users/${id}/deactivation`)
}

export async function reactivateUser(id: string): Promise<void> {
  await httpClient.post(`/users/${id}/reactivation`)
}

export async function listSessions(userId: string): Promise<SessionSummary[]> {
  const { data } = await httpClient.get<SessionSummary[]>(`/users/${userId}/sessions`)

  return data
}

export async function revokeSession(id: string): Promise<void> {
  await httpClient.delete(`/sessions/${id}`)
}

/** "Encerrar todas as outras": o próprio usuário, mantendo a sessão de onde ele está. */
export async function revokeOtherSessions(userId: string): Promise<void> {
  await httpClient.delete(`/users/${userId}/sessions`, { params: { keepCurrent: true } })
}

/** Botão do Administrador sobre outro usuário: encerra todas. */
export async function revokeAllSessions(userId: string): Promise<void> {
  await httpClient.delete(`/users/${userId}/sessions`, { params: { keepCurrent: false } })
}
