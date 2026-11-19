import axios, { AxiosError, type AxiosResponse } from "axios"
import { describe, expect, it } from "vitest"

import { normalizeRejection, toApiError } from "./api-error"

function failure(status: number, data: unknown): AxiosError<never> {
  const error = new AxiosError<never>("falhou")
  error.response = { status, data } as unknown as AxiosResponse<never>
  return error
}

describe("ApiError", () => {
  it("lê código, mensagem e campos inválidos do ProblemDetail", () => {
    const apiError = toApiError(
      failure(400, {
        detail: "dados inválidos",
        code: "invalid_password",
        invalidParams: [{ name: "senha", reason: "curta" }],
      }),
    )

    expect(apiError).toEqual({
      status: 400,
      code: "invalid_password",
      message: "dados inválidos",
      invalidParams: [{ name: "senha", reason: "curta" }],
    })
  })

  it("usa uma mensagem padrão quando a resposta não traz detalhe", () => {
    expect(toApiError(failure(500, null)).message).toBe("algo deu errado; tente de novo")
  })

  it("trata a falta de resposta como falha de rede", () => {
    expect(toApiError(new AxiosError<never>("Network Error"))).toEqual({
      status: 0,
      message: "não foi possível conectar ao servidor",
    })
  })

  it("deixa o cancelamento passar como veio", () => {
    const cancel = new axios.CanceledError("cancelado")

    expect(normalizeRejection(cancel)).toBe(cancel)
  })
})
