#!/usr/bin/env bash
# =============================================================================
# deploy-mocks.sh
# Ejecuta despliegue local: detener servicios, compilar con Maven y relanzar.
# =============================================================================

set -eu
set -o pipefail 2>/dev/null || true

# Unidades systemd
HOTEL_SERVICE="mock-hotel-booking-service"
FLIGHT_SERVICE="mock-flight-booking-service"
TOURISM_SERVICE="mock-tourist-attractions-service"

info()    { echo "[INFO]  $*"; }
success() { echo "[OK]    $*"; }
warn()    { echo "[WARN]  $*"; }
error()   { echo "[ERROR] $*"; exit 1; }

# Forzar ejecucion local desde ns-mock-services con sh deploy-mocks.sh
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
CURRENT_DIR="$(pwd)"
SCRIPT_NAME="$(basename "$0")"

[ "$SCRIPT_NAME" = "deploy-mocks.sh" ] || error "Debes ejecutarlo como: sh deploy-mocks.sh"
[ "${CURRENT_DIR}" = "${SCRIPT_DIR}" ] || error "Debes situarte en ns-mock-services y ejecutarlo con: sh deploy-mocks.sh"

if [ -f "${SCRIPT_DIR}/pom.xml" ]; then
  PROJECT_DIR="${SCRIPT_DIR}"
else
  error "No se encontro pom.xml en ${SCRIPT_DIR}. Ejecuta este script desde ns-mock-services."
fi

# 1) Detener servicios
info "Paso 1/3 - Detener servicios"
for svc in "${HOTEL_SERVICE}" "${FLIGHT_SERVICE}" "${TOURISM_SERVICE}"; do
  if systemctl is-active --quiet "${svc}"; then
    systemctl stop "${svc}"
    success "Servicio detenido: ${svc}"
  else
    warn "Servicio ya estaba detenido o no existe: ${svc}"
  fi
done

# 2) Compilar
info "Paso 2/3 - Compilar modulos"
cd "${PROJECT_DIR}"
mvn -q clean package -DskipTests
success "Compilacion completada"

# 3) Iniciar servicios
info "Paso 3/3 - Iniciar servicios"
for svc in "${HOTEL_SERVICE}" "${FLIGHT_SERVICE}" "${TOURISM_SERVICE}"; do
  systemctl start "${svc}"
  success "Servicio iniciado: ${svc}"
done

echo ""
echo "Verificando arranque de servicios..."
sleep 2
for svc in "${HOTEL_SERVICE}" "${FLIGHT_SERVICE}" "${TOURISM_SERVICE}"; do
  if systemctl is-active --quiet "${svc}"; then
    success "Servicio activo: ${svc}"
  else
    echo ""
    systemctl status "${svc}" --no-pager -l || true
    error "El servicio no quedo activo: ${svc}"
  fi
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
