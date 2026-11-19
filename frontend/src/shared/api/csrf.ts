import { httpClient } from "./http-client"

/**
 * O backend manda o cookie `XSRF-TOKEN` já na primeira resposta (architecture.md §14). Numa aba
 * nova, sem ele, a primeira escrita seria recusada com 403: busca a sessão antes, e ignora o
 * 401 de quem ainda não entrou.
 */
export async function ensureCsrfCookie(): Promise<void> {
  if (document.cookie.split("; ").some((cookie) => cookie.startsWith("XSRF-TOKEN=")))
    return

  await httpClient.get("/auth/me").catch(() => undefined)
}
