import { type RouteConfig, index, layout, route } from "@react-router/dev/routes"

export default [
  route("entrar", "routes/entrar.tsx"),
  route("convite", "routes/convite.tsx"),
  route("esqueci-senha", "routes/esqueci-senha.tsx"),
  route("redefinir-senha", "routes/redefinir-senha.tsx"),
  layout("routes/authenticated.tsx", [
    index("routes/home.tsx"),
    route("conta/trocar-senha", "routes/conta.trocar-senha.tsx"),
  ]),
] satisfies RouteConfig
