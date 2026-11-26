import { ModelsScreen } from "@/features/catalog/screens/models"

import { useCanEditCatalog } from "./catalogo.access"

/** `/catalogo`: a aba Modelos, de todos os papéis; só Administrador e Comercial editam (ADR-6). */
export default function Catalogo() {
  return <ModelsScreen canEdit={useCanEditCatalog()} />
}
