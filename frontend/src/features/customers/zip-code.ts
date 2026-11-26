import { applyMask } from "./display"

/** Máscara `00000-000` ao digitar (ADR-19): só dígitos, no máximo 8. */
export const maskZipCode = (text: string) =>
  applyMask(text.replace(/\D/g, "").slice(0, 8), "#####-###")

/** O CEP guardado (8 dígitos) na tela. */
export const formatZipCode = maskZipCode
