import { ModelFormScreen } from "@/features/catalog/screens/model-form"

import { useCanEditCatalog } from "./catalogo.access"

/** `/catalogo/modelos/:id`: o modelo, editável ou só leitura conforme o papel e a situação. */
export default function CatalogoModelosId() {
  return <ModelFormScreen mode="edit" canEdit={useCanEditCatalog()} />
}
