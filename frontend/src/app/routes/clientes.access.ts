import { useCurrentUser } from "@/features/iam/iam.hooks"

/** ADR-7: Administrador e Comercial editam clientes; o Técnico só consulta. */
export function useCanEditCustomers(): boolean {
  const { data: me } = useCurrentUser()

  return me?.role === "ADMINISTRATOR" || me?.role === "COMMERCIAL"
}
