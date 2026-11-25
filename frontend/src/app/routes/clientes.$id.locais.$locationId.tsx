import { LocationFormScreen } from "@/features/customers/screens/location-form"

import { useCanEditCustomers } from "./clientes.access"

/** `/clientes/:id/locais/:locationId`: a edição do local; quem não edita volta para a ficha. */
export default function ClientesIdLocaisLocationId() {
  return <LocationFormScreen mode="edit" canEdit={useCanEditCustomers()} />
}
