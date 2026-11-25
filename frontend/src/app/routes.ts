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
