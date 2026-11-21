import { ManufacturerPanel } from "@/features/catalog/screens/manufacturer-panel"

import { useCanEditCatalog } from "./catalogo.access"

/** `/catalogo/fabricantes/:id`: a edição do fabricante, no `Outlet` da lista; quem não edita volta para a lista. */
export default function CatalogoFabricantesId() {
  return <ManufacturerPanel mode="edit" canEdit={useCanEditCatalog()} />
}
