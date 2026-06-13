#!/usr/bin/env bash
# =============================================================================
# register-demo-systemd-service.sh
# Registra/actualiza el servicio systemd del demo sin habilitar autostart.
# =============================================================================

set -e

error() { echo "[ERROR] $*"; exit 1; }
success() { echo "[OK]    $*"; }

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
CURRENT_DIR="$(pwd)"
SCRIPT_NAME="$(basename "$0")"

[ "$(id -u)" -eq 0 ] || error "Ejecuta como root: sudo sh register-demo-systemd-service.sh"
[ "$SCRIPT_NAME" = "register-demo-systemd-service.sh" ] || error "Debes ejecutarlo como: sh register-demo-systemd-service.sh"
[ "${CURRENT_DIR}" = "${SCRIPT_DIR}" ] || error "Debes situarte en ns-framework-demo y ejecutarlo con: sudo sh register-demo-systemd-service.sh"
[ -f "${SCRIPT_DIR}/pom.xml" ] || error "No se encontro pom.xml en ${SCRIPT_DIR}."

SERVICE_FILE="/etc/systemd/system/ns-framework-demo.service"
WORK_DIR="${SCRIPT_DIR}"
JAR_PATH="${SCRIPT_DIR}/target/ns-framework-demo-app-0.1.0-SNAPSHOT.jar"

printf '%s\n' \
  '[Unit]' \
  'Description=NS Framework Demo' \
  'After=network.target' \
  '' \
  '[Service]' \
  'Type=simple' \
  'User=root' \
  "WorkingDirectory=${WORK_DIR}" \
  "ExecStart=/usr/bin/java -jar ${JAR_PATH}" \
  'SuccessExitStatus=143' \
  'Restart=on-failure' \
  'RestartSec=5' \
  '' \
  '[Install]' \
  'WantedBy=multi-user.target' \
  > "${SERVICE_FILE}"

systemctl daemon-reload
systemctl disable ns-framework-demo >/dev/null 2>&1 || true

success "ns-framework-demo registrado/actualizado en systemd"
echo "Service file: ${SERVICE_FILE}"
echo "Para desplegar: sh deploy-demo.sh"

