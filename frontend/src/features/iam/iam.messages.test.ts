import { describe, expect, it } from "vitest"

import { LABELS, messageFor, sentence } from "./iam.messages"

// A tabela de erros de docs/system-design/01-iam/usecases.md.
const TABLE: Record<string, string> = {
  invalid_credentials: "e-mail ou senha incorretos",
  session_invalid: "sua sessão não vale mais; entre de novo",
  rate_limited: "muitas tentativas; tente de novo em alguns minutos",
  password_change_required: "você precisa trocar a senha antes de continuar",
  forbidden: "você não tem permissão para isto",
  link_expired: "este link expirou",
  link_used: "este link já foi usado ou foi substituído",
  email_already_registered: "já existe um usuário com este e-mail",
  user_not_found: "usuário não encontrado",
  session_not_found: "sessão não encontrada",
  user_not_invited: "este usuário não está com convite pendente",
  user_not_active: "este usuário não está ativo",
  user_deactivated: "este usuário já está desativado",
  user_not_deactivated: "este usuário não está desativado",
  cannot_change_own_role: "você não pode mudar o seu próprio papel",
  cannot_deactivate_self: "você não pode desativar a sua própria conta",
  wrong_current_password: "a senha atual está incorreta",
}

describe("messageFor", () => {
  it.each(Object.entries(TABLE))("%s tem a mensagem da tabela", (code, message) => {
    expect(messageFor(code, "outra")).toBe(message)
  })

  it("devolve o fallback para código desconhecido ou ausente", () => {
    expect(messageFor("nao_existe", "tente de novo")).toBe("tente de novo")
    expect(messageFor(undefined, "tente de novo")).toBe("tente de novo")
  })

  it("invalid_password devolve o fallback, que traz o critério que falta", () => {
    expect(messageFor("invalid_password", "a senha precisa de um número")).toBe(
      "a senha precisa de um número",
    )
  })
})

describe("LABELS", () => {
  it("traduz papel e situação", () => {
    expect(LABELS.role).toEqual({
      ADMINISTRATOR: "Administrador",
      COMMERCIAL: "Comercial",
      TECHNICIAN: "Técnico",
    })
    expect(LABELS.status).toEqual({
      ACTIVE: "Ativo",
      INVITED: "Convite pendente",
      DEACTIVATED: "Desativado",
    })
  })
})

describe("sentence", () => {
  it("põe a inicial em maiúscula e mantém o resto", () => {
    expect(sentence("a senha precisa ter um número")).toBe(
      "A senha precisa ter um número",
    )
    expect(sentence("Já está")).toBe("Já está")
    expect(sentence("")).toBe("")
  })
})
