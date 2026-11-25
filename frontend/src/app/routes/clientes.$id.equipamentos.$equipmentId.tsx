import { EquipmentFormScreen } from "@/features/customers/screens/equipment-form"

import { useCanEditCustomers } from "./clientes.access"

/** `/clientes/:id/equipamentos/:equipmentId`: a edição do equipamento; quem não edita volta para a ficha. */
export default function ClientesIdEquipamentosEquipmentId() {
  return <EquipmentFormScreen mode="edit" canEdit={useCanEditCustomers()} />
}
