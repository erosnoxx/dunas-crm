import { CustomersScreen } from "@/features/customers/screens/customers"

import { useCanEditCustomers } from "./clientes.access"

/** `/clientes`: a lista, de todos os papéis; só Administrador e Comercial editam (ADR-7). */
export default function Clientes() {
  return <CustomersScreen canEdit={useCanEditCustomers()} />
}
