import type { CustomerSummary } from "../customers.service"
import { orderCities } from "../display"

const SHOWN = 2

/**
 * As cidades da lista (telas 1 e 2): até duas, a buscada primeiro, e "+N" com o texto para o
 * leitor de tela; "Nenhum local" sem cidade.
 */
function CityList({ cities, text }: { cities: CustomerSummary["cities"]; text: string }) {
  if (cities.length === 0) return <span className="text-text-muted">Nenhum local</span>

  const ordered = orderCities(cities, text)
  const rest = ordered.length - SHOWN

  return (
    <span>
      {ordered
        .slice(0, SHOWN)
        .map(({ city, state }) => `${city}/${state}`)
        .join(", ")}
      {rest > 0 && (
        <span className="ml-1.5 inline-block bg-chip-ok px-1.5 text-caption leading-4.5 font-semibold text-text">
          <span aria-hidden="true">+{rest}</span>
          <span className="sr-only">
            e mais {rest} {rest === 1 ? "cidade" : "cidades"}
          </span>
        </span>
      )}
    </span>
  )
}

export { CityList }
