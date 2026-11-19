import { describe, expect, it, vi } from "vitest"

import { createQueryClient, shouldRetry } from "./query-client"

describe("shouldRetry", () => {
  it("não repete erro de negócio (4xx)", () => {
    expect(shouldRetry(0, { status: 404, message: "não encontrado" })).toBe(false)
  })

  it("repete uma vez a falha de rede", () => {
    expect(shouldRetry(0, { status: 0, message: "rede" })).toBe(true)
    expect(shouldRetry(1, { status: 0, message: "rede" })).toBe(false)
  })
})

const sessionInvalid = { status: 401, code: "session_invalid", message: "sessão" }

function fail(client: ReturnType<typeof createQueryClient>, key: string, error: unknown) {
  return client
    .fetchQuery({ queryKey: [key], queryFn: () => Promise.reject(error) })
    .catch(() => undefined)
}

describe("createQueryClient: sessão que cai", () => {
  it("limpa o cache e avisa uma única vez, mesmo com várias consultas falhando", async () => {
    const onSessionInvalid = vi.fn()
    const client = createQueryClient(onSessionInvalid)
    client.setQueryData(["users"], [1])

    await Promise.all([
      fail(client, "a", sessionInvalid),
      fail(client, "b", sessionInvalid),
    ])

    expect(onSessionInvalid).toHaveBeenCalledTimes(1)
    expect(client.getQueryData(["users"])).toBeUndefined()
  })

  it("trata a mutation autenticada que recebe session_invalid", async () => {
    const onSessionInvalid = vi.fn()
    const client = createQueryClient(onSessionInvalid)

    await client
      .getMutationCache()
      .build(client, { mutationFn: () => Promise.reject(sessionInvalid) })
      .execute(undefined)
      .catch(() => undefined)

    expect(onSessionInvalid).toHaveBeenCalledTimes(1)
  })

  it("não age em outro código, nem em outro status", async () => {
    const onSessionInvalid = vi.fn()
    const client = createQueryClient(onSessionInvalid)

    await fail(client, "a", { status: 401, code: "invalid_credentials", message: "x" })
    await fail(client, "b", { status: 403, code: "session_invalid", message: "x" })
    await fail(client, "c", { status: 0, message: "rede" })

    expect(onSessionInvalid).not.toHaveBeenCalled()
  })

  it("não age na consulta que cuida da própria sessão (meta handlesOwnSession)", async () => {
    const onSessionInvalid = vi.fn()
    const client = createQueryClient(onSessionInvalid)

    await client
      .fetchQuery({
        queryKey: ["me"],
        queryFn: () => Promise.reject(sessionInvalid),
        meta: { handlesOwnSession: true },
      })
      .catch(() => undefined)

    expect(onSessionInvalid).not.toHaveBeenCalled()
  })

  it("trata session_invalid de um me que já tinha dado: a sessão caiu e o cache guardado não vale", async () => {
    const onSessionInvalid = vi.fn()
    const client = createQueryClient(onSessionInvalid)
    client.setQueryData(["me"], { id: "1" })

    await client
      .fetchQuery({
        queryKey: ["me"],
        queryFn: () => Promise.reject(sessionInvalid),
        meta: { handlesOwnSession: true },
      })
      .catch(() => undefined)

    expect(onSessionInvalid).toHaveBeenCalledTimes(1)
    expect(client.getQueryData(["me"])).toBeUndefined()
  })

  it("volta a agir depois que a pessoa entra de novo", async () => {
    const onSessionInvalid = vi.fn()
    const client = createQueryClient(onSessionInvalid)

    await fail(client, "a", sessionInvalid)
    await client.fetchQuery({ queryKey: ["ok"], queryFn: () => "ok" })
    await fail(client, "b", sessionInvalid)

    expect(onSessionInvalid).toHaveBeenCalledTimes(2)
  })
})
