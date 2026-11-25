import { applyMask } from "./display"

// DDD sem zero inicial; fixo com 8 dígitos, celular com 9 começando por 9 (ADR-18).
const BRAZILIAN = /^[1-9]\d(\d{8}|9\d{8})$/
// O formato do `Phone` do backend: o número estrangeiro guardado só passa de volta, não se digita.
const E164 = /^\+[1-9]\d{6,14}$/
const STORED_BRAZILIAN = /^\+55\d{10,11}$/

const digitsOf = (text: string) => text.replace(/\D/g, "")

/** Máscara `(00) 0000-0000` ou `(00) 00000-0000` ao digitar. */
export function maskPhone(text: string): string {
  const digits = digitsOf(text).slice(0, 11)

  return applyMask(digits, digits.length === 11 ? "(##) #####-####" : "(##) ####-####")
}

/** O que vai à API: `+55` + DDD + número, o estrangeiro guardado como está, `null` se vazio. */
export function toE164(value: string): string | null {
  const text = value.trim()

  if (text.startsWith("+")) return text
  if (text === "") return null

  return `+55${digitsOf(text)}`
}

export function checkPhone(value: string): boolean {
  const text = value.trim()

  if (text.startsWith("+")) return E164.test(text)

  return BRAZILIAN.test(digitsOf(text))
}

export const isBrazilian = (stored: string) => STORED_BRAZILIAN.test(stored)

/** O telefone guardado na tela: o `+55` volta com a máscara; outro país, como está (ADR-18). */
export const formatPhone = (stored: string) =>
  isBrazilian(stored) ? maskPhone(stored.slice(3)) : stored
