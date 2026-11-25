import { FORM_MESSAGES } from "./customers.messages"
import type { DocumentType } from "./customers.service"
import { applyMask } from "./display"

/** Como o `Document` do backend guarda: só letras e dígitos, em maiúsculas. */
export const normalizeDocument = (text: string) =>
  text.replace(/[^a-zA-Z0-9]/g, "").toUpperCase()

/** O tipo vem do tamanho, como no backend: 11 é CPF, 14 é CNPJ. */
export function documentTypeOf(text: string): DocumentType | undefined {
  const length = normalizeDocument(text).length

  if (length === 11) return "CPF"
  if (length === 14) return "CNPJ"

  return undefined
}

/** Máscara ao digitar e formatação do guardado: CPF enquanto couber em 11 dígitos, senão CNPJ (que aceita letras). */
export function formatDocument(value: string): string {
  const chars = normalizeDocument(value).slice(0, 14)
  const isCpf = chars.length <= 11 && /^\d*$/.test(chars)

  return applyMask(chars, isCpf ? "###.###.###-##" : "##.###.###/####-##")
}

const allSame = (value: string) => [...value].every((char) => char === value[0])

const remainderDigit = (sum: number) => (sum % 11 < 2 ? 0 : 11 - (sum % 11))

// Porto do `CpfValidator` do backend.
function isValidCpf(value: string): boolean {
  if (!/^\d{11}$/.test(value) || allSame(value)) return false

  const digits = [...value].map(Number)
  const digit = (length: number) =>
    remainderDigit(
      digits.slice(0, length).reduce((sum, d, i) => sum + d * (length + 1 - i), 0),
    )

  return digit(9) === digits[9] && digit(10) === digits[10]
}

const CNPJ_WEIGHTS = [6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2]

// Porto do `CnpjValidator` do backend, alfanumérico: a letra vale o código menos 48 ("A" = 17).
function isValidCnpj(value: string): boolean {
  if (!/^[A-Z0-9]{14}$/.test(value) || allSame(value)) return false

  const values = [...value].map((char) => char.charCodeAt(0) - 48)
  const digit = (length: number) =>
    remainderDigit(
      values
        .slice(0, length)
        .reduce((sum, v, i) => sum + v * CNPJ_WEIGHTS[i + 13 - length], 0),
    )

  return digit(12) === values[12] && digit(13) === values[13]
}

export type DocumentCheck = { ok: true; value: string } | { ok: false; message: string }

/** A conferência do formulário (ADR-24), com as mensagens da tela; `value` é o documento normalizado. */
export function checkDocument(text: string): DocumentCheck {
  const value = normalizeDocument(text)
  const type = documentTypeOf(value)

  if (value === "") return { ok: false, message: FORM_MESSAGES.documentRequired }
  if (type === undefined) return { ok: false, message: FORM_MESSAGES.documentIncomplete }
  if (type === "CPF" && !isValidCpf(value))
    return { ok: false, message: FORM_MESSAGES.cpfInvalid }
  if (type === "CNPJ" && !isValidCnpj(value))
    return { ok: false, message: FORM_MESSAGES.cnpjInvalid }

  return { ok: true, value }
}
