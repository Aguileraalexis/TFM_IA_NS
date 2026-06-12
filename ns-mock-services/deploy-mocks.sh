#!/usr/bin/env bash
# =============================================================================
# deploy-mocks.sh
# Descarga (o actualiza) ns-mock-services desde GitHub, detiene los contenedores
# existentes, recompila las imágenes y arranca los servicios mock.
# =============================================================================

set -euo pipefail

# ---------------------------------------------------------------------------
# CONFIGURACIÓN — edita estas variables antes de ejecutar
# ---------------------------------------------------------------------------
GITHUB_REPO_URL="https://github.com/YOUR_GITHUB_OWNER/YOUR_REPO.git"  # URL del repositorio
TARGET_DIR="ns-mock-services"   # carpeta local donde se clona/actualiza
BRANCH="main"                   # rama a usar
# ---------------------------------------------------------------------------

BOLD="\033[1m"
GREEN="\033[0;32m"
YELLOW="\033[0;33m"
RED="\033[0;31m"
RESET="\033[0m"

info()    { echo -e "${BOLD}[INFO]${RESET}  $*"; }
success() { echo -e "${GREEN}[OK]${RESET}    $*"; }
warn()    { echo -e "${YELLOW}[WARN]${RESET}  $*"; }
error()   { echo -e "${RED}[ERROR]${RESET} $*" >&2; exit 1; }

# ---------------------------------------------------------------------------
# 0. Comprobaciones previas
# ---------------------------------------------------------------------------
command -v git          >/dev/null 2>&1 || error "git no está instalado."
command -v docker       >/dev/null 2>&1 || error "docker no está instalado."
command -v docker compose >/dev/null 2>&1 || \
  docker compose version >/dev/null 2>&1 || \
  error "docker compose no está disponible."

# ---------------------------------------------------------------------------
# 1. Descargar / actualizar el repositorio
# ---------------------------------------------------------------------------
info "Paso 1/4 — Descargar código desde GitHub..."

if [ -d "${TARGET_DIR}/.git" ]; then
  info "Repositorio ya existe en '${TARGET_DIR}'. Actualizando..."
  git -C "${TARGET_DIR}" fetch origin
  git -C "${TARGET_DIR}" checkout "${BRANCH}"
  git -C "${TARGET_DIR}" reset --hard "origin/${BRANCH}"
  success "Repositorio actualizado a la última versión de '${BRANCH}'."
else
  info "Clonando ${GITHUB_REPO_URL} en '${TARGET_DIR}'..."
  git clone --branch "${BRANCH}" --depth 1 "${GITHUB_REPO_URL}" "${TARGET_DIR}"
  success "Repositorio clonado correctamente."
fi

# Entrar al directorio donde está el docker-compose.yml
cd "${TARGET_DIR}"

# ---------------------------------------------------------------------------
# 2. Detener y eliminar los contenedores en ejecución
# ---------------------------------------------------------------------------
info "Paso 2/4 — Deteniendo servicios mock en ejecución..."

if docker compose ps --quiet 2>/dev/null | grep -q .; then
  docker compose down --remove-orphans
  success "Contenedores detenidos y eliminados."
else
  warn "No había contenedores en ejecución."
fi

# ---------------------------------------------------------------------------
# 3. Compilar imágenes Docker (incluye mvn package dentro del Dockerfile)
# ---------------------------------------------------------------------------
info "Paso 3/4 — Compilando imágenes Docker (esto puede tardar varios minutos)..."

docker compose build --no-cache
success "Imágenes compiladas correctamente."

# ---------------------------------------------------------------------------
# 4. Arrancar los servicios en segundo plano
# ---------------------------------------------------------------------------
info "Paso 4/4 — Arrancando servicios mock..."

docker compose up -d
success "Servicios iniciados en segundo plano."

# ---------------------------------------------------------------------------
# 5. Resumen de estado
# ---------------------------------------------------------------------------
echo ""
echo -e "${BOLD}============================================================${RESET}"
echo -e "${BOLD} Servicios mock en ejecución${RESET}"
echo -e "${BOLD}============================================================${RESET}"
docker compose ps
echo ""
echo -e "  ${GREEN}mock-hotel-booking-service${RESET}      → http://localhost:8082"
echo -e "  ${GREEN}mock-flight-booking-service${RESET}     → http://localhost:8084"
echo -e "  ${GREEN}mock-tourist-attractions-service${RESET} → http://localhost:8085"
echo ""
echo -e "  Logs en tiempo real: ${BOLD}docker compose logs -f${RESET}"
echo -e "  Detener servicios:   ${BOLD}docker compose down${RESET}"
echo -e "${BOLD}============================================================${RESET}"

