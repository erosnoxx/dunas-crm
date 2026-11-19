export const PASSWORD_MAX = 30
const PASSWORD_MIN = 8

export type PasswordRule = {
  id: "length" | "upper" | "lower" | "digit" | "special"
  label: string
  met: boolean
}

/**
 * Espelha o `Password` do backend (ADR-3), caso a caso: a senha é medida como veio, sem
 * `trim` (aparar alteraria a senha). `ç` é minúscula, `€` é especial e espaço não é especial.
 */
export function checkPassword(password: string): PasswordRule[] {
  return [
    {
      id: "length",
      label: "De 8 a 30 caracteres",
      met: password.length >= PASSWORD_MIN && password.length <= PASSWORD_MAX,
    },
    { id: "upper", label: "Uma letra maiúscula", met: /\p{Lu}/u.test(password) },
    { id: "lower", label: "Uma letra minúscula", met: /\p{Ll}/u.test(password) },
    { id: "digit", label: "Um número", met: /\p{Nd}/u.test(password) },
    {
      id: "special",
      label: "Um caractere especial, como ! ou #",
      met: /[^\p{L}\p{Nd}\s]/u.test(password),
    },
  ]
}

export function isPasswordValid(password: string): boolean {
  return checkPassword(password).every((rule) => rule.met)
}
