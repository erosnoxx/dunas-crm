import { LocationFormScreen } from "@/features/customers/screens/location-form"

import { useCanEditCustomers } from "./clientes.access"

/** `/clientes/:id/locais/novo`: o cadastro do local; quem não edita volta para a ficha. */
export default function ClientesIdLocaisNovo() {
  return <LocationFormScreen mode="new" canEdit={useCanEditCustomers()} />
}
