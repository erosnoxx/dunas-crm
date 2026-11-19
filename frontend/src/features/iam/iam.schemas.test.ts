import { describe, expect, it } from "vitest"

import {
  acceptInvitationSchema,
  changePasswordSchema,
  forgotPasswordSchema,
  inviteUserSchema,
  resetPasswordSchema,
  signInSchema,
} from "./iam.schemas"

const GOOD = "Dunas#2026"

function messages(result: {
  success: boolean
  error?: { issues: { message: string }[] }
}) {
  return result.error?.issues.map((issue) => issue.message) ?? []
}

describe("signInSchema", () => {
  it("aceita e-mail e senha, e mantém os espaços das pontas da senha", () => {
    const result = signInSchema.safeParse({ email: "a@b.com", password: " Dunas#2026 " })

    expect(result.success).toBe(true)
    expect(result.data?.password).toBe(" Dunas#2026 ")
  })

  it("recusa e-mail sem @, e-mail com mais de 254 e senha vazia", () => {
    expect(signInSchema.safeParse({ email: "ab.com", password: GOOD }).success).toBe(
      false,
    )
    expect(
      signInSchema.safeParse({ email: `${"a".repeat(250)}@b.com`, password: GOOD })
        .success,
    ).toBe(false)
    expect(signInSchema.safeParse({ email: "a@b.com", password: "" }).success).toBe(false)
  })

  it("recusa senha de 31 caracteres", () => {
    expect(
      signInSchema.safeParse({ email: "a@b.com", password: "a".repeat(31) }).success,
    ).toBe(false)
  })
})

describe("senha nova", () => {
  it("acceptInvitationSchema recusa 31 caracteres, regra não cumprida e repeat diferente", () => {
    const long = `Dunas#${"a".repeat(25)}1`
    expect(
      acceptInvitationSchema.safeParse({ password: long, repeat: long }).success,
    ).toBe(false)
    expect(
      acceptInvitationSchema.safeParse({ password: "dunas#2026", repeat: "dunas#2026" })
        .success,
    ).toBe(false)

    const mismatch = acceptInvitationSchema.safeParse({
      password: GOOD,
      repeat: "Dunas#2027",
    })
    expect(mismatch.success).toBe(false)
    expect(mismatch.error?.issues[0]?.path).toEqual(["repeat"])
  })

  it("acceptInvitationSchema mostra o repeat diferente mesmo com a senha fora das regras", () => {
    const result = acceptInvitationSchema.safeParse({
      password: "dunas",
      repeat: "outra",
    })

    expect(
      result.error?.issues.some(
        (i) => i.path[0] === "repeat" && i.message === "as senhas não são iguais",
      ),
    ).toBe(true)
  })

  it("acceptInvitationSchema não apara: espaços nas pontas permanecem", () => {
    const password = " Dunas#2026 "
    const result = acceptInvitationSchema.safeParse({ password, repeat: password })

    expect(result.success).toBe(true)
    expect(result.data).toEqual({ password, repeat: password })
  })

  it("resetPasswordSchema segue a mesma regra", () => {
    expect(resetPasswordSchema.safeParse({ password: GOOD, repeat: GOOD }).success).toBe(
      true,
    )
    expect(resetPasswordSchema.safeParse({ password: GOOD, repeat: "x" }).success).toBe(
      false,
    )
    expect(
      resetPasswordSchema.safeParse({ password: "curta", repeat: "curta" }).success,
    ).toBe(false)
  })

  it("changePasswordSchema exige a atual e a nova igual à repetida, sem aparar", () => {
    const input = {
      currentPassword: " Velha#2026 ",
      newPassword: " Nova#2026 ",
      repeat: " Nova#2026 ",
    }
    const ok = changePasswordSchema.safeParse(input)

    expect(ok.success).toBe(true)
    expect(ok.data).toEqual(input)
    expect(
      changePasswordSchema.safeParse({ ...input, currentPassword: "" }).success,
    ).toBe(false)
    expect(
      changePasswordSchema.safeParse({ ...input, repeat: "Nova#2027" }).success,
    ).toBe(false)
    expect(
      changePasswordSchema.safeParse({ ...input, newPassword: "fraca", repeat: "fraca" })
        .success,
    ).toBe(false)
  })

  it("dá mensagem em pt-BR", () => {
    const result = acceptInvitationSchema.safeParse({ password: GOOD, repeat: "outra" })

    expect(messages(result)).toEqual(["as senhas não são iguais"])
  })
})

describe("forgotPasswordSchema", () => {
  it("exige e-mail válido", () => {
    expect(forgotPasswordSchema.safeParse({ email: "a@b.com" }).success).toBe(true)
    expect(forgotPasswordSchema.safeParse({ email: "ab.com" }).success).toBe(false)
  })
})

describe("inviteUserSchema", () => {
  const valid = { name: "Ana", email: "ana@dunas.com", role: "COMMERCIAL" }

  it("aceita nome de 2 a 80 caracteres", () => {
    expect(inviteUserSchema.safeParse(valid).success).toBe(true)
    expect(inviteUserSchema.safeParse({ ...valid, name: "A".repeat(80) }).success).toBe(
      true,
    )
  })

  it("recusa nome de 1 e de 81 caracteres", () => {
    expect(inviteUserSchema.safeParse({ ...valid, name: "A" }).success).toBe(false)
    expect(inviteUserSchema.safeParse({ ...valid, name: "A".repeat(81) }).success).toBe(
      false,
    )
  })

  it("recusa e-mail sem @ e papel desconhecido", () => {
    expect(inviteUserSchema.safeParse({ ...valid, email: "ana.dunas.com" }).success).toBe(
      false,
    )
    expect(inviteUserSchema.safeParse({ ...valid, role: "ROOT" }).success).toBe(false)
  })
})
