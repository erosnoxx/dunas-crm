import { useSyncExternalStore } from "react"

/** A partir de `md` (48rem): o mesmo ponto de quebra das utilidades `md:` do Tailwind. */
export const MD_UP = "(min-width: 48rem)"

/** `true` enquanto a media query vale; reage quando a tela cruza o breakpoint. */
export function useMediaQuery(query: string): boolean {
  return useSyncExternalStore(
    (notify) => {
      const list = window.matchMedia(query)

      list.addEventListener("change", notify)

      return () => list.removeEventListener("change", notify)
    },
    () => window.matchMedia(query).matches,
    () => false,
  )
}
