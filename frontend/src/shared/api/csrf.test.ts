import { afterEach, describe, expect, it, vi } from "vitest"

import { ensureCsrfCookie } from "./csrf"
import { httpClient } from "./http-client"

afterEach(() => {
  vi.restoreAllMocks()
  document.cookie = "XSRF-TOKEN=; expires=Thu, 01 Jan 1970 00:00:00 GMT"
})

describe("ensureCsrfCookie", () => {
  it("não faz requisição quando o cookie já existe", async () => {
    document.cookie = "XSRF-TOKEN=abc"
    const get = vi.spyOn(httpClient, "get")

    await ensureCsrfCookie()

    expect(get).not.toHaveBeenCalled()
  })

  it("busca a sessão quando o cookie não existe e ignora o 401", async () => {
    const get = vi
      .spyOn(httpClient, "get")
      .mockRejectedValue({ status: 401, message: "não autenticado" })

    await expect(ensureCsrfCookie()).resolves.toBeUndefined()

    expect(get).toHaveBeenCalledWith("/auth/me")
  })
})
