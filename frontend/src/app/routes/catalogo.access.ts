import { useCurrentUser } from "@/features/iam/iam.hooks"

/** ADR-6: Administrador e Comercial editam o catálogo; o Técnico só consulta. */
export function useCanEditCatalog(): boolean {
  const { data: me } = useCurrentUser()

  return me?.role === "ADMINISTRATOR" || me?.role === "COMMERCIAL"
}
