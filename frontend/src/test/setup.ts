import "@testing-library/jest-dom/vitest"
import { configure } from "@testing-library/react"

// O padrão de 1 s do findBy e do waitFor estoura na primeira renderização a frio, com workers em paralelo.
configure({ asyncUtilTimeout: 5000 })

// O jsdom não tem matchMedia, que o sonner usa para seguir o tema do aparelho.
if (!window.matchMedia) {
  window.matchMedia = (query: string) =>
    ({
      matches: false,
      media: query,
      onchange: null,
      addEventListener: () => undefined,
      removeEventListener: () => undefined,
      addListener: () => undefined,
      removeListener: () => undefined,
      dispatchEvent: () => false,
    }) as MediaQueryList
}
