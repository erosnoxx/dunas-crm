import { TypePanel } from "@/features/catalog/screens/type-panel"

import { useCanEditCatalog } from "./catalogo.access"

/** `/catalogo/tipos/:id`: a edição do tipo, no `Outlet` da lista; quem não edita volta para a lista. */
export default function CatalogoTiposId() {
  return <TypePanel mode="edit" canEdit={useCanEditCatalog()} />
}
