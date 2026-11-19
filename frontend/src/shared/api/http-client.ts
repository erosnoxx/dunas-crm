import axios from "axios"

import { normalizeRejection } from "./api-error"

type TokenProvider = () => string | null

let tokenProvider: TokenProvider | null = null

/**
 * Cliente HTTP único do frontend (docs/rules/frontend.md §5). No navegador a sessão é um cookie
 * httpOnly que o JavaScript nunca vê: o cliente só garante que o cookie viaja
 * (`withCredentials`) e que toda escrita leva o cabeçalho de CSRF, no padrão
 * cookie-para-cabeçalho do Spring Security.
 */
export const httpClient = axios.create({
  baseURL: "/api",
  withCredentials: true,
  xsrfCookieName: "XSRF-TOKEN",
  xsrfHeaderName: "X-XSRF-TOKEN",
})

/**
 * Ponto de extensão do aplicativo (Capacitor): quem tem token guardado no aparelho o informa
 * aqui e as requisições passam a levar `Authorization: Bearer`. Na web ninguém o chama, e
 * nenhuma requisição leva o cabeçalho.
 */
export function setTokenProvider(provider: TokenProvider | null): void {
  tokenProvider = provider
}

httpClient.interceptors.request.use((config) => {
  const token = tokenProvider?.()

  if (token) config.headers.set("Authorization", `Bearer ${token}`)

  return config
})

httpClient.interceptors.response.use(
  (response) => response,
  (error) => Promise.reject(normalizeRejection(error)),
)
