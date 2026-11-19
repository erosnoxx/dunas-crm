import type { CSSProperties } from "react"
import { Toaster as Sonner, type ToasterProps } from "sonner"

/** Avisos curtos. Seguem o tema do aparelho e usam os tokens do design system. */
function Toaster(props: ToasterProps) {
  return (
    <Sonner
      theme="system"
      style={
        {
          "--normal-bg": "var(--panel)",
          "--normal-text": "var(--text)",
          "--normal-border": "var(--line-strong)",
          "--border-radius": "0px",
        } as CSSProperties
      }
      {...props}
    />
  )
}

export { Toaster }
