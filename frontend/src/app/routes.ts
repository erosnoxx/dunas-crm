import { type RouteConfig, index, layout, route } from "@react-router/dev/routes"

export default [
  route("entrar", "routes/entrar.tsx"),
  route("convite", "routes/convite.tsx"),
  route("esqueci-senha", "routes/esqueci-senha.tsx"),
  route("redefinir-senha", "routes/redefinir-senha.tsx"),
  layout("routes/authenticated.tsx", [
    index("routes/home.tsx"),
    route("usuarios", "routes/usuarios.tsx", [
      // Estática, irmã de `:id`: `/usuarios/convidar` não vira o detalhe de um usuário "convidar".
      route("convidar", "routes/usuarios.convidar.tsx"),
      route(":id", "routes/usuarios.$id.tsx"),
    ]),
    // A lista; as estáticas de clientes (`clientes/novo`) entram antes de `clientes/:id`.
    route("clientes", "routes/clientes._index.tsx"),
    // Estática antes de `:id`: `/clientes/novo` não vira o cliente "novo".
    route("clientes/novo", "routes/clientes.novo.tsx"),
    route("clientes/:id", "routes/clientes.$id.tsx"),
    route("clientes/:id/editar", "routes/clientes.$id.editar.tsx"),
    // Estática antes de `:locationId`: `/clientes/:id/locais/novo` não vira o local "novo".
    route("clientes/:id/locais/novo", "routes/clientes.$id.locais.novo.tsx"),
    route(
      "clientes/:id/locais/:locationId",
      "routes/clientes.$id.locais.$locationId.tsx",
    ),
    // O equipamento novo nasce no local (`.../equipamentos/novo`, estática, depois do local); o
    // salvo se abre pelo cliente, sem o local na rota (ADR-22: a edição lê da ficha).
    route(
      "clientes/:id/locais/:locationId/equipamentos/novo",
      "routes/clientes.$id.locais.$locationId.equipamentos.novo.tsx",
    ),
    route(
      "clientes/:id/equipamentos/:equipmentId",
      "routes/clientes.$id.equipamentos.$equipmentId.tsx",
    ),
    route("catalogo", "routes/catalogo._index.tsx"),
    // Estática antes de `:id`: `/catalogo/modelos/novo` não vira o modelo "novo".
    route("catalogo/modelos/novo", "routes/catalogo.modelos.novo.tsx"),
    route("catalogo/modelos/:id", "routes/catalogo.modelos.$id.tsx"),
    route("catalogo/fabricantes", "routes/catalogo.fabricantes.tsx", [
      // Estática, irmã de `:id`: `/catalogo/fabricantes/novo` não vira o fabricante "novo".
      route("novo", "routes/catalogo.fabricantes.novo.tsx"),
      route(":id", "routes/catalogo.fabricantes.$id.tsx"),
    ]),
    route("catalogo/tipos", "routes/catalogo.tipos.tsx", [
      // Estática, irmã de `:id`: `/catalogo/tipos/novo` não vira o tipo "novo".
      route("novo", "routes/catalogo.tipos.novo.tsx"),
      route(":id", "routes/catalogo.tipos.$id.tsx"),
    ]),
    route("conta/sessoes", "routes/conta.sessoes.tsx"),
    route("conta/trocar-senha", "routes/conta.trocar-senha.tsx"),
  ]),
] satisfies RouteConfig
