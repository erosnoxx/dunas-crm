import type { InternalAxiosRequestConfig } from "axios"
import { afterEach, describe, expect, it } from "vitest"

import { httpClient, setTokenProvider } from "./http-client"

afterEach(() => setTokenProvider(null))

async function sentHeaders(): Promise<Record<string, unknown>> {
  let sent: InternalAxiosRequestConfig | undefined

  await httpClient.get("/auth/me", {
    adapter: async (config) => {
      sent = config
      return { data: {}, status: 200, statusText: "OK", headers: {}, config }
    },
  })

  return sent?.headers.toJSON() ?? {}
}

describe("httpClient", () => {
  it("manda o cookie junto com a requisição", () => {
    expect(httpClient.defaults.withCredentials).toBe(true)
  })

  it("não manda Authorization por padrão, como no navegador", async () => {
    expect(await sentHeaders()).not.toHaveProperty("Authorization")
  })

  it("manda Authorization quando o provedor devolve um token", async () => {
    setTokenProvider(() => "abc")

    expect(await sentHeaders()).toHaveProperty("Authorization", "Bearer abc")
  })

  it("não manda Authorization quando o provedor não tem token", async () => {
    setTokenProvider(() => null)

    expect(await sentHeaders()).not.toHaveProperty("Authorization")
  })
})
