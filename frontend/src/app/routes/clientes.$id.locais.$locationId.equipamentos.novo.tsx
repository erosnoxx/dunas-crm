import { EquipmentFormScreen } from "@/features/customers/screens/equipment-form"

import { useCanEditCustomers } from "./clientes.access"

/** `/clientes/:id/locais/:locationId/equipamentos/novo`: o cadastro do equipamento; quem não edita volta para a ficha. */
export default function ClientesIdLocaisLocationIdEquipamentosNovo() {
  return <EquipmentFormScreen mode="new" canEdit={useCanEditCustomers()} />
}
