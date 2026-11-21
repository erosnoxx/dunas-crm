import { TypesScreen } from "@/features/catalog/screens/types"

import { useCanEditCatalog } from "./catalogo.access"

/**
 * `/catalogo/tipos`: a lista, de todos os papéis. É a rota de layout de `novo` e `:id`: a tela
 * renderiza o `Outlet` na região do painel.
 */
export default function CatalogoTipos() {
  return <TypesScreen canEdit={useCanEditCatalog()} />
}
