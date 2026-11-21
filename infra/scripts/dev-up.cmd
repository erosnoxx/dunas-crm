@echo off
setlocal
rem ============================================================================
rem  crm: sobe o stack completo de desenvolvimento
rem
rem  Rebuilda backend e frontend com o codigo atual e espera todo servico ficar
rem  saudavel antes de devolver o terminal. Funciona de qualquer pasta.
rem
rem      infra\scripts\dev-up.cmd
rem ============================================================================

set "ROOT=%~dp0.."
set "ENV_FILE=%ROOT%\envs\.env.dev"
set "COMPOSE_FILE=%ROOT%\docker\compose.dev.yml"

if not exist "%ENV_FILE%" (
  echo.
  echo  [crm] Arquivo infra\envs\.env.dev nao encontrado.
  echo        Copie infra\envs\.env.example para .env.dev e ajuste os valores.
  echo.
  exit /b 1
)

docker info >nul 2>&1
if errorlevel 1 (
  echo.
  echo  [crm] O Docker nao esta rodando. Abra o Docker Desktop e tente de novo.
  echo.
  exit /b 1
)

echo.
echo  [crm] Subindo o stack de desenvolvimento...
echo.

docker compose ^
  --env-file "%ENV_FILE%" ^
  -f "%COMPOSE_FILE%" ^
  up --build --detach --wait --remove-orphans

if errorlevel 1 (
  echo.
  echo  [crm] Algum servico nao ficou saudavel. Veja o que aconteceu com:
  echo        infra\scripts\dev-logs.cmd
  echo.
  exit /b 1
)

for /f "usebackq tokens=1,* delims==" %%A in ("%ENV_FILE%") do (
  if "%%A"=="FRONTEND_HOST_PORT" set "FRONTEND_PORT=%%B"
  if "%%A"=="API_HOST_PORT" set "API_PORT=%%B"
  if "%%A"=="MAILPIT_UI_HOST_PORT" set "MAILPIT_PORT=%%B"
  if "%%A"=="RABBIT_UI_HOST_PORT" set "RABBIT_PORT=%%B"
  if "%%A"=="CRM_ADMIN_EMAIL" set "ADMIN_EMAIL=%%B"
)
if not defined FRONTEND_PORT set "FRONTEND_PORT=8090"
if not defined API_PORT set "API_PORT=8080"
if not defined MAILPIT_PORT set "MAILPIT_PORT=8025"
if not defined RABBIT_PORT set "RABBIT_PORT=15672"

echo.
echo  [crm] Stack no ar.
echo.
echo     App        http://localhost:%FRONTEND_PORT%
echo     Swagger    http://localhost:%API_PORT%/swagger-ui.html
echo     E-mails    http://localhost:%MAILPIT_PORT%
echo     RabbitMQ   http://localhost:%RABBIT_PORT%
echo.
echo     Admin      %ADMIN_EMAIL%  (senha em infra\envs\.env.dev, CRM_ADMIN_PASSWORD)
echo.
echo     Logs:      infra\scripts\dev-logs.cmd [servico]
echo     Parar:     infra\scripts\dev-down.cmd
echo.

endlocal
