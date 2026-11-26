import { TypePanel } from "@/features/catalog/screens/type-panel"

import { useCanEditCatalog } from "./catalogo.access"

/** `/catalogo/tipos/novo`: o cadastro, painel na web e tela cheia no celular; quem não edita volta para a lista. */
export default function CatalogoTiposNovo() {
  return <TypePanel mode="new" canEdit={useCanEditCatalog()} />
}
