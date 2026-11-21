import { ManufacturersScreen } from "@/features/catalog/screens/manufacturers"

import { useCanEditCatalog } from "./catalogo.access"

/**
 * `/catalogo/fabricantes`: a lista, de todos os papéis. É a rota de layout de `novo` e `:id`: a
 * tela renderiza o `Outlet` na região do painel.
 */
export default function CatalogoFabricantes() {
  return <ManufacturersScreen canEdit={useCanEditCatalog()} />
}
