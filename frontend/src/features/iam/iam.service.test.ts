import { beforeEach, describe, expect, it, vi } from "vitest"

import { ensureCsrfCookie } from "@/shared/api/csrf"
import { httpClient } from "@/shared/api/http-client"

import * as service from "./iam.service"

vi.mock("@/shared/api/http-client", () => ({
  httpClient: { get: vi.fn(), post: vi.fn(), put: vi.fn(), delete: vi.fn() },
}))
vi.mock("@/shared/api/csrf", () => ({ ensureCsrfCookie: vi.fn() }))

const http = vi.mocked(httpClient)
const csrf = vi.mocked(ensureCsrfCookie)
const ID = "7f1c0a52-0000-4000-8000-000000000001"

function reply(data: unknown = undefined) {
  return { data }
}

beforeEach(() => {
  vi.resetAllMocks()
  for (const verb of ["get", "post", "put", "delete"] as const)
    http[verb].mockResolvedValue(reply())
})

describe("sessão e senha", () => {
  it("login: garante o CSRF, faz POST /auth/login e devolve só a SessionView", async () => {
    http.post.mockResolvedValue(
      reply({
        expiresAt: "2026-12-19T10:00:00Z",
        mustChangePassword: false,
        token: "segredo",
      }),
    )

    const session = await service.login({ email: "a@b.com", password: " Dunas#2026 " })

    expect(csrf).toHaveBeenCalledBefore(http.post)
    expect(http.post).toHaveBeenCalledWith("/auth/login", {
      email: "a@b.com",
      password: " Dunas#2026 ",
    })
    expect(session).toEqual({
      expiresAt: "2026-12-19T10:00:00Z",
      mustChangePassword: false,
    })
    expect(session).not.toHaveProperty("token")
  })

  it("acceptInvitation: garante o CSRF, faz POST e não lê token", async () => {
    http.post.mockResolvedValue(
      reply({
        expiresAt: "2026-12-19T10:00:00Z",
        mustChangePassword: false,
        token: "segredo",
      }),
    )

    const session = await service.acceptInvitation({ token: "t", password: "Dunas#2026" })

    expect(csrf).toHaveBeenCalledBefore(http.post)
    expect(http.post).toHaveBeenCalledWith("/auth/invitation/accept", {
      token: "t",
      password: "Dunas#2026",
    })
    expect(session).not.toHaveProperty("token")
  })

  it("requestPasswordReset: garante o CSRF e faz POST /auth/password/forgot", async () => {
    await service.requestPasswordReset({ email: "a@b.com" })

    expect(csrf).toHaveBeenCalledBefore(http.post)
    expect(http.post).toHaveBeenCalledWith("/auth/password/forgot", { email: "a@b.com" })
  })

  it("resetPassword: garante o CSRF e faz POST /auth/password/reset", async () => {
    await service.resetPassword({ token: "t", password: "Dunas#2026" })

    expect(csrf).toHaveBeenCalledBefore(http.post)
    expect(http.post).toHaveBeenCalledWith("/auth/password/reset", {
      token: "t",
      password: "Dunas#2026",
    })
  })

  it("logout faz POST /auth/logout", async () => {
    await service.logout()

    expect(http.post).toHaveBeenCalledWith("/auth/logout")
  })

  it("changePassword faz PUT /auth/password", async () => {
    await service.changePassword({
      currentPassword: "Velha#2026",
      newPassword: "Nova#2026",
    })

    expect(http.put).toHaveBeenCalledWith("/auth/password", {
      currentPassword: "Velha#2026",
      newPassword: "Nova#2026",
    })
  })

  it("getCurrentUser faz GET /auth/me e devolve o corpo", async () => {
    const me = {
      id: ID,
      name: "Ana",
      email: "a@b.com",
      role: "ADMINISTRATOR",
      mustChangePassword: false,
    }
    http.get.mockResolvedValue(reply(me))

    expect(await service.getCurrentUser()).toEqual(me)
    expect(http.get).toHaveBeenCalledWith("/auth/me")
  })
})

describe("usuários", () => {
  it("listUsers faz GET /users com a página", async () => {
    const page = { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 }
    http.get.mockResolvedValue(reply(page))

    expect(await service.listUsers({ page: 0, size: 20 })).toEqual(page)
    expect(http.get).toHaveBeenCalledWith("/users", { params: { page: 0, size: 20 } })
  })

  it("getUser faz GET /users/{id}", async () => {
    await service.getUser(ID)

    expect(http.get).toHaveBeenCalledWith(`/users/${ID}`)
  })

  it("inviteUser faz POST /users e devolve o id", async () => {
    http.post.mockResolvedValue(reply({ id: ID }))

    expect(
      await service.inviteUser({ name: "Ana", email: "a@b.com", role: "COMMERCIAL" }),
    ).toEqual({
      id: ID,
    })
    expect(http.post).toHaveBeenCalledWith("/users", {
      name: "Ana",
      email: "a@b.com",
      role: "COMMERCIAL",
    })
  })

  it("resendInvitation faz POST /users/{id}/invitation", async () => {
    await service.resendInvitation(ID)

    expect(http.post).toHaveBeenCalledWith(`/users/${ID}/invitation`)
  })

  it("changeUserRole faz PUT /users/{id}/role", async () => {
    await service.changeUserRole(ID, "TECHNICIAN")

    expect(http.put).toHaveBeenCalledWith(`/users/${ID}/role`, { role: "TECHNICIAN" })
  })

  it("deactivateUser e reactivateUser fazem POST", async () => {
    await service.deactivateUser(ID)
    await service.reactivateUser(ID)

    expect(http.post).toHaveBeenCalledWith(`/users/${ID}/deactivation`)
    expect(http.post).toHaveBeenCalledWith(`/users/${ID}/reactivation`)
  })
})

describe("sessões", () => {
  it("listSessions faz GET /users/{id}/sessions", async () => {
    await service.listSessions(ID)

    expect(http.get).toHaveBeenCalledWith(`/users/${ID}/sessions`)
  })

  it("revokeSession faz DELETE /sessions/{id}", async () => {
    await service.revokeSession(ID)

    expect(http.delete).toHaveBeenCalledWith(`/sessions/${ID}`)
  })

  it("revokeOtherSessions mantém a atual; revokeAllSessions não", async () => {
    await service.revokeOtherSessions(ID)
    await service.revokeAllSessions(ID)

    expect(http.delete).toHaveBeenCalledWith(`/users/${ID}/sessions`, {
      params: { keepCurrent: true },
    })
    expect(http.delete).toHaveBeenCalledWith(`/users/${ID}/sessions`, {
      params: { keepCurrent: false },
    })
  })
})
