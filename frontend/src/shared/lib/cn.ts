import { createCn } from "cn/config"

/**
 * `cn` com a escala tipográfica do tema (`@utility text-*` de `styles/theme.css`) registrada como
 * tamanho de fonte: sem isso, `text-label` parece uma cor e some ao lado de `text-text`.
 */
export const cn = createCn({
  extend: {
    classGroups: {
      "font-size": [
        {
          text: [
            "display-xl",
            "display-lg",
            "title",
            "number",
            "body",
            "body-sm",
            "label",
            "caption",
          ],
        },
      ],
    },
  },
})
