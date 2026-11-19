import { QueryClient } from "@tanstack/react-query"

import type { ApiError } from "@/shared/api/api-error"

const MAX_RETRIES = 1

/**
 * Erro de negócio ou de validação (4xx) não melhora na segunda tentativa; falha de rede
 * (`status: 0`) e 5xx podem ser transitórias e repetem uma vez.
 */
export function shouldRetry(failureCount: number, error: unknown): boolean {
  const status = (error as Partial<ApiError>).status ?? 0

  if (status >= 400 && status < 500) return false

  return failureCount < MAX_RETRIES
}

export const queryClient = new QueryClient({
  defaultOptions: {
    queries: { retry: shouldRetry, refetchOnWindowFocus: false },
  },
})
