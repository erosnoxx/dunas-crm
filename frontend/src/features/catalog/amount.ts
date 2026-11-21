const FORMAT = /^\d{1,9}(,\d{1,3})?$/

export type ParsedAmount = { ok: true; value: string } | { ok: false; reason: string }

/** O que a pessoa digita (vírgula decimal) vira o texto com ponto que a API espera. */
export function parseAmount(input: string): ParsedAmount {
  if (input === "") return { ok: false, reason: "informe o valor" }
  if (!FORMAT.test(input))
    return {
      ok: false,
      reason:
        "use só dígitos e uma vírgula, com até 9 inteiros e 3 decimais, sem ponto nem espaço",
    }

  const value = input.replace(",", ".")

  if (Number(value) <= 0)
    return { ok: false, reason: "o valor precisa ser maior que zero" }

  return { ok: true, value }
}

/** O valor da API na tela: vírgula decimal, sem milhar e sem zeros à direita. */
export function formatAmount(value: string | number): string {
  const text = String(value)
  const trimmed = text.includes(".") ? text.replace(/0+$/, "").replace(/\.$/, "") : text

  return trimmed.replace(".", ",")
}
