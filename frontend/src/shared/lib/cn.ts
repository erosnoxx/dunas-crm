import { createCn } from "cn/config"

/**
 * A escala tipográfica do tema (`@utility text-*` de `styles/theme.css`, nomes do `tokens.json`).
 * `cn.test.ts` falha se um nome entrar no tema sem entrar aqui.
 */
export const TYPE_SCALE = [
  "display-xl",
  "display-lg",
  "title",
  "number",
  "body",
  "body-sm",
  "label",
  "caption",
]

/**
 * `cn` com a escala registrada como tamanho de fonte: sem isso, `text-label` parece uma cor e
 * some ao lado de `text-text`.
 */
export const cn = createCn({
  extend: { classGroups: { "font-size": [{ text: TYPE_SCALE }] } },
})
