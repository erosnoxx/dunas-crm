import type { Config } from "@react-router/dev/config"

export default {
  appDirectory: "src/app",
  // SPA: nenhuma página pública precisa de SEO. O framework mode deixa ligar pré-render por rota
  // depois, sem trocar de ferramenta (docs/rules/frontend.md §2).
  ssr: false,
} satisfies Config
