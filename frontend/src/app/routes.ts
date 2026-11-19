import { type RouteConfig, index, layout, route } from "@react-router/dev/routes"

export default [
  route("entrar", "routes/entrar.tsx"),
  route("convite", "routes/convite.tsx"),
  layout("routes/authenticated.tsx", [index("routes/home.tsx")]),
] satisfies RouteConfig
