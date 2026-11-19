import { Logo } from "@/shared/ui/logo"

/** Página provisória: a primeira tela aprovada (feature 01-iam) a substitui. */
export default function Home() {
  return (
    <main className="flex min-h-screen flex-col items-center justify-center gap-4 p-6">
      <Logo size={48} />
      <p className="text-body text-text-muted">Em construção</p>
    </main>
  )
}
