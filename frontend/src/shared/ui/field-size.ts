/**
 * Tamanhos dos campos de formulário (`Input` e `NativeSelect`). O padrão é 52 px no celular e
 * 48 px a partir do `md`. O compacto é o dos formulários do catálogo no mockup: 48 px e 44 px,
 * com 12 px de respiro, fundo `ground` sobre o painel a partir do `md`, contorno de erro de
 * 2 px (o respiro cai para 11 px para o texto não andar) e fundo `nav` quando desabilitado.
 */
export const FIELD_SIZES = {
  default: "h-13 px-3.5 md:h-12",
  compact:
    "h-12 px-3 md:h-11 md:bg-ground disabled:bg-nav md:disabled:bg-nav aria-invalid:border-2 aria-invalid:px-2.75",
} as const

export type FieldSize = keyof typeof FIELD_SIZES
