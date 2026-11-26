import { ManufacturerPanel } from "@/features/catalog/screens/manufacturer-panel"

import { useCanEditCatalog } from "./catalogo.access"

/** `/catalogo/fabricantes/novo`: o cadastro, painel na web e tela cheia no celular; quem não edita volta para a lista. */
export default function CatalogoFabricantesNovo() {
  return <ManufacturerPanel mode="new" canEdit={useCanEditCatalog()} />
}
