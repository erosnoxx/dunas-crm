import { z } from "zod"

import { isPasswordValid, PASSWORD_MAX } from "./password-rules"

export const EMAIL_MAX = 254
const NAME_MIN = 2
export const NAME_MAX = 80

// Nenhum campo de senha usa `.trim()`: aparar alteraria a senha digitada.
const email = z
  .string()
  .trim()
  .min(1, "informe o e-mail")
  .max(EMAIL_MAX, `o e-mail tem no máximo ${EMAIL_MAX} caracteres`)
  .email("informe um e-mail válido")

const currentPassword = z
  .string()
  .min(1, "informe a senha")
  .max(PASSWORD_MAX, `a senha tem no máximo ${PASSWORD_MAX} caracteres`)

const newPassword = z
  .string()
  .max(PASSWORD_MAX, `a senha tem no máximo ${PASSWORD_MAX} caracteres`)
  .refine(isPasswordValid, "a senha não cumpre as regras")

const MISMATCH = { path: ["repeat"], message: "as senhas não são iguais" }
export const signInSchema = z.object({ email, password: currentPassword })

export const acceptInvitationSchema = z
  .object({ password: newPassword, repeat: z.string() })
  .refine((v) => v.password === v.repeat, MISMATCH)

export const resetPasswordSchema = acceptInvitationSchema

export const forgotPasswordSchema = z.object({ email })

export const changePasswordSchema = z
  .object({ currentPassword, newPassword, repeat: z.string() })
  .refine((v) => v.newPassword === v.repeat, MISMATCH)

export const inviteUserSchema = z.object({
  name: z
    .string()
    .trim()
    .min(NAME_MIN, `o nome tem no mínimo ${NAME_MIN} caracteres`)
    .max(NAME_MAX, `o nome tem no máximo ${NAME_MAX} caracteres`),
  email,
  role: z.enum(["ADMINISTRATOR", "COMMERCIAL", "TECHNICIAN"], {
    error: "escolha o papel",
  }),
})

export type SignInValues = z.infer<typeof signInSchema>
export type AcceptInvitationValues = z.infer<typeof acceptInvitationSchema>
export type ForgotPasswordValues = z.infer<typeof forgotPasswordSchema>
export type ChangePasswordValues = z.infer<typeof changePasswordSchema>
export type InviteUserValues = z.infer<typeof inviteUserSchema>
