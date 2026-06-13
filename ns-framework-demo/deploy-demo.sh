#!/usr/bin/env bash
# =============================================================================
# deploy-demo.sh
# Ejecuta despliegue local del demo: detener servicio, compilar y relanzar.
# =============================================================================

set -eu
set -o pipefail 2>/dev/null || true

DEMO_SERVICE="ns-framework-demo"
SERVICE_FILE_ETC="/etc/systemd/system/${DEMO_SERVICE}.service"
SERVICE_FILE_USR_LIB="/usr/lib/systemd/system/${DEMO_SERVICE}.service"
SERVICE_FILE_LIB="/lib/systemd/system/${DEMO_SERVICE}.service"

info()    { echo "[INFO]  $*"; }
success() { echo "[OK]    $*"; }
warn()    { echo "[WARN]  $*"; }
error()   { echo "[ERROR] $*"; exit 1; }

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
CURRENT_DIR="$(pwd)"
SCRIPT_NAME="$(basename "$0")"

[ "$SCRIPT_NAME" = "deploy-demo.sh" ] || error "Debes ejecutarlo como: sh deploy-demo.sh"
[ "${CURRENT_DIR}" = "${SCRIPT_DIR}" ] || error "Debes situarte en ns-framework-demo y ejecutarlo con: sh deploy-demo.sh"
[ -f "${SCRIPT_DIR}/pom.xml" ] || error "No se encontro pom.xml en ${SCRIPT_DIR}."

if [ ! -f "${SERVICE_FILE_ETC}" ] && [ ! -f "${SERVICE_FILE_USR_LIB}" ] && [ ! -f "${SERVICE_FILE_LIB}" ]; then
  error "No existe ${DEMO_SERVICE}.service. Ejecuta antes: sudo sh register-demo-systemd-service.sh"
fi

# Refrescar unidades para asegurar que systemd vea el .service recien registrado.
systemctl daemon-reload

# 1) Detener servicio
info "Paso 1/3 - Detener servicio demo"
if systemctl is-active --quiet "${DEMO_SERVICE}"; then
  systemctl stop "${DEMO_SERVICE}"
  success "Servicio detenido: ${DEMO_SERVICE}"
else
  warn "Servicio ya estaba detenido: ${DEMO_SERVICE}"
fi

# 2) Compilar
info "Paso 2/3 - Compilar demo"
cd "${SCRIPT_DIR}"
mvn -q clean package -DskipTests
success "Compilacion completada"

# 3) Iniciar servicio
info "Paso 3/3 - Iniciar servicio demo"
systemctl start "${DEMO_SERVICE}"
success "Servicio iniciado: ${DEMO_SERVICE}"

echo ""
echo "Verificando arranque del servicio..."
sleep 2
if systemctl is-active --quiet "${DEMO_SERVICE}"; then
  success "Servicio activo: ${DEMO_SERVICE}"
else
  echo ""
  systemctl status "${DEMO_SERVICE}" --no-pager -l || true
  error "El servicio no quedo activo: ${DEMO_SERVICE}"
fi

echo ""
echo "Estado del servicio:"
systemctl status "${DEMO_SERVICE}" --no-pager -l || true

echo ""
echo "Endpoint esperado:"
echo "- Demo: http://localhost:8080"

