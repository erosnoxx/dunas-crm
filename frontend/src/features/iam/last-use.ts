const MINUTE = 60_000
const HOUR = 60 * MINUTE
const DAY = 24 * HOUR

const relative = new Intl.RelativeTimeFormat("pt-BR", { numeric: "always" })

/** "agora", "há 5 minutos", "há 3 horas", "há 2 dias". Relógio torto (futuro) conta como agora. */
export function lastUse(iso: string, now: Date): string {
  const elapsed = now.getTime() - new Date(iso).getTime()

  if (elapsed < MINUTE) return "agora"
  if (elapsed < HOUR) return relative.format(-Math.floor(elapsed / MINUTE), "minute")
  if (elapsed < DAY) return relative.format(-Math.floor(elapsed / HOUR), "hour")

  return relative.format(-Math.floor(elapsed / DAY), "day")
}
