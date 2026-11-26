import axios, { type AxiosError } from "axios"

export type InvalidParam = {
  name: string
  reason: string
}

/**
 * Forma única de erro que o resto do frontend enxerga: nunca AxiosError nem o ProblemDetail
 * cru. Cobre erro de negócio (com `code`), validação (400, com `invalidParams`) e falha de
 * rede (`status: 0`). A tela decide pelo `code`, nunca pelo texto (architecture.md §7.3).
 */
export type ApiError = {
  status: number
  code?: string
  message: string
  invalidParams?: InvalidParam[]
}

type ProblemDetail = {
  status?: number
  detail?: string
  code?: string
  invalidParams?: InvalidParam[]
}

const NETWORK_ERROR_MESSAGE = "não foi possível conectar ao servidor"
export const UNKNOWN_ERROR_MESSAGE = "algo deu errado; tente de novo"

export function toApiError(error: AxiosError<ProblemDetail>): ApiError {
  if (!error.response) return { status: 0, message: NETWORK_ERROR_MESSAGE }

  const { status, data } = error.response

  return {
    status,
    code: data?.code,
    message: data?.detail ?? UNKNOWN_ERROR_MESSAGE,
    invalidParams: data?.invalidParams,
  }
}

/**
 * O que o interceptor do cliente HTTP rejeita. Cancelamento (o `AbortController` que o TanStack
 * Query usa ao desmontar um componente) passa como veio: não é erro do servidor nem falha de
 * rede, e não pode virar aviso para a pessoa.
 */
export function normalizeRejection(error: unknown): unknown {
  if (axios.isCancel(error)) return error

  return toApiError(error as AxiosError<ProblemDetail>)
}
