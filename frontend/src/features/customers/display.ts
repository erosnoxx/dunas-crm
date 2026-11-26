import type { CustomerSummary } from "./customers.service"

/** Encaixa os caracteres no molde (`#` é um caractere), parando onde eles acabam: serve à máscara parcial. */
export function applyMask(chars: string, pattern: string): string {
  let result = ""
  let index = 0

  for (const slot of pattern) {
    if (index === chars.length) break

    if (slot === "#") {
      result += chars[index]
      index += 1
      continue
    }

    result += slot
  }

  return result
}

/** O `aria-describedby` dos ids presentes, ou nada. */
export const describedBy = (...ids: (string | false | undefined)[]) =>
  ids.filter(Boolean).join(" ") || undefined

/** "2025-03-12" (como a API manda a data) vira "12/03/2025". */
export function formatDate(value: string): string {
  const [year, month, day] = value.split("-")

  return `${day}/${month}/${year}`
}

/** O valor da API na tela: vírgula decimal, sem milhar e sem zeros à direita, como o do catálogo. */
export function formatAmount(value: string | number): string {
  const text = String(value)
  const trimmed = text.includes(".") ? text.replace(/0+$/, "").replace(/\.$/, "") : text

  return trimmed.replace(".", ",")
}

type City = CustomerSummary["cities"][number]

/** A cidade que casou com a busca vem primeiro, para o motivo do resultado aparecer; depois, por nome (design/README). */
export function orderCities(cities: City[], text: string): City[] {
  const query = text.trim().toLowerCase()
  const matches = (city: City) => query !== "" && city.city.toLowerCase().includes(query)

  return [...cities].sort(
    (a, b) =>
      Number(matches(b)) - Number(matches(a)) || a.city.localeCompare(b.city, "pt-BR"),
  )
}
