import { CustomerDetailScreen } from "@/features/customers/screens/customer-detail"

import { useCanEditCustomers } from "./clientes.access"

/** `/clientes/:id`: a ficha do cliente, de todos os papéis; só quem edita vê as ações (ADR-7). */
export default function ClientesId() {
  return <CustomerDetailScreen canEdit={useCanEditCustomers()} />
}
