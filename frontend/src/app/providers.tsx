import { useState, type ReactNode } from "react"
import { QueryClientProvider } from "@tanstack/react-query"
import { useNavigate } from "react-router"

import { createQueryClient } from "@/shared/lib/query-client"
import { Toaster } from "@/shared/ui/sonner"

/** Providers globais: cache de dados e avisos. Sessão que cai no meio do uso leva ao login. */
export function Providers({ children }: { children: ReactNode }) {
  // O `navigate` do roteador de dados tem identidade estável: guardar o primeiro basta.
  const navigate = useNavigate()
  const [client] = useState(() =>
    createQueryClient(() => {
      void navigate("/entrar", { replace: true })
    }),
  )

  return (
    <QueryClientProvider client={client}>
      {children}
      <Toaster position="top-center" />
    </QueryClientProvider>
  )
}
