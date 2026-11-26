# syntax=docker/dockerfile:1
#
# Imagem do frontend: SPA (react-router, ssr: false) servida pelo nginx, que tambem faz o
# proxy de /api para a API. Frontend e API no mesmo site, sem CORS (frontend.md §5).
#
# Contexto de build: frontend/. O nginx.conf vem do contexto adicional "nginx"
# (infra/docker/context/nginx), declarado em compose.dev.yml.

# --- build ---------------------------------------------------------------------
FROM node:22-alpine AS build

WORKDIR /src

COPY package.json package-lock.json ./
RUN --mount=type=cache,target=/root/.npm npm ci

COPY . .

RUN npm run build

# --- runtime -------------------------------------------------------------------
FROM nginx:1.28-alpine

COPY --from=nginx nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /src/build/client /usr/share/nginx/html

EXPOSE 80

HEALTHCHECK --interval=10s --timeout=5s --retries=5 \
  CMD wget -q --spider http://127.0.0.1/ || exit 1
