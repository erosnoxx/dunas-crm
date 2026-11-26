import { ModelFormScreen } from "@/features/catalog/screens/model-form"

import { useCanEditCatalog } from "./catalogo.access"

/** `/catalogo/modelos/novo`: o cadastro do modelo; quem não edita volta para a lista. */
export default function CatalogoModelosNovo() {
  return <ModelFormScreen mode="new" canEdit={useCanEditCatalog()} />
}
