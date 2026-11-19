import {
  isRouteErrorResponse,
  Links,
  Meta,
  Outlet,
  Scripts,
  ScrollRestoration,
} from "react-router"

import "@/styles/fonts"
import "@/styles/theme.css"

import { Providers } from "./providers"
import type { Route } from "./+types/root"

export function Layout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="pt-BR">
      <head>
        <meta charSet="utf-8" />
        <meta name="viewport" content="width=device-width, initial-scale=1" />
        <title>DUNAS</title>
        <Meta />
        <Links />
      </head>
      <body>
        {children}
        <ScrollRestoration />
        <Scripts />
      </body>
    </html>
  )
}

export function HydrateFallback() {
  return null
}

export default function App() {
  return (
    <Providers>
      <Outlet />
    </Providers>
  )
}

export function ErrorBoundary({ error }: Route.ErrorBoundaryProps) {
  const notFound = isRouteErrorResponse(error) && error.status === 404

  return (
    <main className="flex min-h-screen flex-col items-center justify-center gap-2 p-6">
      <h1 className="text-display-lg font-display">
        {notFound ? "Página não encontrada" : "Algo deu errado"}
      </h1>
      <p className="text-body text-text-muted">
        {notFound ? "O endereço não existe." : "Tente de novo em instantes."}
      </p>
    </main>
  )
}
