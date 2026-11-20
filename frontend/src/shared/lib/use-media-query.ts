import { useSyncExternalStore } from "react"

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
