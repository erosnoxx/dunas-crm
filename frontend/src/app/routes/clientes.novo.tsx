import { CustomerFormScreen } from "@/features/customers/screens/customer-form"

import { useCanEditCustomers } from "./clientes.access"

/** `/clientes/novo`: o cadastro do cliente; quem não edita volta para a lista. */
export default function ClientesNovo() {
  return <CustomerFormScreen mode="new" canEdit={useCanEditCustomers()} />
}
