import { MutationCache, QueryCache, QueryClient } from "@tanstack/react-query"

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

declare module "@tanstack/react-query" {
  interface Register {
    queryMeta: {
      /** A consulta decide sozinha o que fazer sem sessão (a que pergunta quem está logado). */
      handlesOwnSession?: boolean
    }
  }
}

function isSessionInvalid(error: unknown): boolean {
  const { status, code } = error as Partial<ApiError>

  return status === 401 && code === "session_invalid"
}

/**
 * Cliente de consulta do app. `session_invalid` de qualquer consulta ou mutation autenticada
 * (a sessão caiu no meio do uso) limpa o cache e chama `onSessionInvalid` **uma única vez**: as
 * demais consultas em voo falham com o mesmo erro e não repetem o aviso. A trava solta quando
 * algo dá certo de novo (a pessoa entrou). A consulta marcada `handlesOwnSession` fica de
 * fora: é ela que a tela pública de login faz sem sessão, e que o guarda lê.
 */
export function createQueryClient(onSessionInvalid: () => void): QueryClient {
  let handled = false

  const onSuccess = () => {
    handled = false
  }
  const onError = (error: unknown) => {
    if (handled || !isSessionInvalid(error)) return

    handled = true
    client.clear()
    onSessionInvalid()
  }

  const client = new QueryClient({
    queryCache: new QueryCache({
      onSuccess,
      onError: (error, query) => {
        if (!query.meta?.handlesOwnSession) onError(error)
      },
    }),
    mutationCache: new MutationCache({ onSuccess, onError }),
    defaultOptions: {
      queries: { retry: shouldRetry, refetchOnWindowFocus: false },
    },
  })

  return client
}
