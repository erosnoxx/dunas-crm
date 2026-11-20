@echo off
setlocal
rem ============================================================================
rem  crm: acompanha os logs do stack de desenvolvimento (Ctrl+C sai)
rem
rem      infra\scripts\dev-logs.cmd            todos os servicos
rem      infra\scripts\dev-logs.cmd backend    so um servico (backend, frontend,
rem                                            postgres, rabbitmq, redis, mailpit)
rem ============================================================================

set "ROOT=%~dp0.."
set "ENV_FILE=%ROOT%\envs\.env.dev"
set "COMPOSE_FILE=%ROOT%\docker\compose.dev.yml"

docker compose ^
  --env-file "%ENV_FILE%" ^
  -f "%COMPOSE_FILE%" ^
  logs --follow --tail 200 %*

endlocal
