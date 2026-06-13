#!/usr/bin/env bash
# =============================================================================
# deploy-mocks.sh
# Actualiza codigo desde GitHub, detiene servicios systemd, compila y reinicia.
# Flujo operativo: cd + git + mvn + systemctl.
# =============================================================================

set -euo pipefail

# ---------------------------------------------------------------------------
# CONFIGURACION
# ---------------------------------------------------------------------------
GITHUB_REPO_URL="https://github.com/Aguileraalexis/TFM_IA_NS.git"
TARGET_DIR="ns-mock-services"
BRANCH="master"

# Unidades systemd
HOTEL_SERVICE="mock-hotel-booking-service"
FLIGHT_SERVICE="mock-flight-booking-service"
TOURISM_SERVICE="mock-tourist-attractions-service"
# ---------------------------------------------------------------------------

info()    { echo "[INFO]  $*"; }
success() { echo "[OK]    $*"; }
warn()    { echo "[WARN]  $*"; }

# 1) Descargar o actualizar repositorio
info "Paso 1/4 - Descargar o actualizar codigo"
if [ -d "${TARGET_DIR}/.git" ]; then
  cd "${TARGET_DIR}"
  git fetch origin
  git checkout "${BRANCH}"
  git reset --hard "origin/${BRANCH}"
  success "Repositorio actualizado en rama ${BRANCH}"
else
  git clone --branch "${BRANCH}" --depth 1 "${GITHUB_REPO_URL}" "${TARGET_DIR}"
  cd "${TARGET_DIR}"
  success "Repositorio clonado"
fi

# 2) Detener servicios
info "Paso 2/4 - Detener servicios"
for svc in "${HOTEL_SERVICE}" "${FLIGHT_SERVICE}" "${TOURISM_SERVICE}"; do
  if systemctl is-active --quiet "${svc}"; then
    systemctl stop "${svc}"
    success "Servicio detenido: ${svc}"
  else
    warn "Servicio ya estaba detenido o no existe: ${svc}"
  fi
done

# 3) Compilar
info "Paso 3/4 - Compilar modulos"
mvn -q clean package -DskipTests
success "Compilacion completada"

# 4) Iniciar servicios
info "Paso 4/4 - Iniciar servicios"
for svc in "${HOTEL_SERVICE}" "${FLIGHT_SERVICE}" "${TOURISM_SERVICE}"; do
  systemctl start "${svc}"
  success "Servicio iniciado: ${svc}"
done

echo ""
echo "Estado de servicios:"
systemctl status "${HOTEL_SERVICE}" --no-pager -l || true
systemctl status "${FLIGHT_SERVICE}" --no-pager -l || true
systemctl status "${TOURISM_SERVICE}" --no-pager -l || true

echo ""
echo "Endpoints esperados:"
echo "- Hotel:   http://localhost:8082"
echo "- Flight:  http://localhost:8084"
echo "- Tourism: http://localhost:8085"
