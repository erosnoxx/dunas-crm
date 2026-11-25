import { Loader2Icon } from "lucide-react"

function Spinner() {
  return (
    <div
      role="status"
      aria-label="Carregando"
      aria-busy="true"
      className="flex justify-center py-6"
    >
      <Loader2Icon aria-hidden="true" className="size-6 animate-spin text-text-muted" />
    </div>
  )
}

export { Spinner }
