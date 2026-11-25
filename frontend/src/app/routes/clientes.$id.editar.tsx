import { CustomerFormScreen } from "@/features/customers/screens/customer-form"

import { useCanEditCustomers } from "./clientes.access"

/** `/clientes/:id/editar`: a edição do cliente; quem não edita volta para a ficha. */
export default function ClientesIdEditar() {
  return <CustomerFormScreen mode="edit" canEdit={useCanEditCustomers()} />
}
