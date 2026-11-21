@echo off
setlocal
rem ============================================================================
rem  crm: derruba o stack de desenvolvimento
rem
rem      infra\scripts\dev-down.cmd          para e remove os conteineres; os dados
rem                                          do Postgres e do RabbitMQ ficam
rem      infra\scripts\dev-down.cmd --reset  apaga tambem os volumes: banco zerado
rem                                          (e administrador recriado) na proxima subida
rem ============================================================================

set "ROOT=%~dp0.."
set "ENV_FILE=%ROOT%\envs\.env.dev"
set "COMPOSE_FILE=%ROOT%\docker\compose.dev.yml"

set "VOLUMES="
if /i "%~1"=="--reset" set "VOLUMES=--volumes"

docker compose ^
  --env-file "%ENV_FILE%" ^
  -f "%COMPOSE_FILE%" ^
  down --remove-orphans %VOLUMES%

if /i "%~1"=="--reset" (
  echo.
  echo  [crm] Stack derrubado e volumes apagados.
) else (
  echo.
  echo  [crm] Stack derrubado. Os dados continuam nos volumes.
)
echo.

endlocal
