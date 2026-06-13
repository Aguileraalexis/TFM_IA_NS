#!/usr/bin/env bash
# Register systemd services for 3 mock apps + demo app.
# Usage:
#   sudo sh register-systemd-services.sh --base-dir /opt/ns-tesis/src/TFM_IA_NS --start

set -eu
set -o pipefail 2>/dev/null || true

START_SERVICES="false"
BASE_DIR=""
RUN_USER="${SUDO_USER:-}"
JAVA_BIN="/usr/bin/java"

info() { echo "[INFO]  $*"; }
ok()   { echo "[OK]    $*"; }
warn() { echo "[WARN]  $*"; }
err()  { echo "[ERROR] $*"; exit 1; }

usage() {
  cat <<'EOF'
Usage:
  sudo sh register-systemd-services.sh [--base-dir <path>] [--user <linux-user>] [--java-bin <path>] [--start]

Options:
  --base-dir  Base folder that contains ns-mock-services and ns-framework-demo
  --user      Linux user that will run the services (default: SUDO_USER)
  --java-bin  Java binary path (default: /usr/bin/java)
  --start     Start services after enable
EOF
}

while [ "$#" -gt 0 ]; do
  case "$1" in
    --base-dir)
      [ "$#" -ge 2 ] || err "Missing value for --base-dir"
      BASE_DIR="$2"
      shift 2
      ;;
    --user)
      [ "$#" -ge 2 ] || err "Missing value for --user"
      RUN_USER="$2"
      shift 2
      ;;
    --java-bin)
      [ "$#" -ge 2 ] || err "Missing value for --java-bin"
      JAVA_BIN="$2"
      shift 2
      ;;
    --start)
      START_SERVICES="true"
      shift
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      err "Unknown option: $1"
      ;;
  esac
done

[ "$(id -u)" -eq 0 ] || err "Run as root (sudo)."
command -v systemctl >/dev/null 2>&1 || err "systemctl not found"
[ -x "$JAVA_BIN" ] || err "Java binary not executable: $JAVA_BIN"

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
if [ -z "$BASE_DIR" ]; then
  BASE_DIR="$SCRIPT_DIR"
fi

MOCKS_DIR="$BASE_DIR/ns-mock-services"
DEMO_DIR="$BASE_DIR/ns-framework-demo"

[ -d "$MOCKS_DIR" ] || err "Folder not found: $MOCKS_DIR"
[ -d "$DEMO_DIR" ] || err "Folder not found: $DEMO_DIR"

if [ -z "$RUN_USER" ]; then
  RUN_USER="root"
  warn "--user not set and SUDO_USER empty; using root"
fi

resolve_jar() {
  dir="$1"
  pattern="$2"
  jar=""
  for candidate in "$dir"/$pattern; do
    if [ -f "$candidate" ]; then
      case "$candidate" in
        *original-*)
          ;;
        *)
          jar="$candidate"
          ;;
      esac
    fi
  done
  [ -n "$jar" ] && echo "$jar" && return 0
  return 1
}

write_unit() {
  svc_name="$1"
  desc="$2"
  work_dir="$3"
  jar_file="$4"
  unit_file="/etc/systemd/system/${svc_name}.service"

  cat > "$unit_file" <<EOF
[Unit]
Description=$desc
After=network.target

[Service]
Type=simple
User=$RUN_USER
WorkingDirectory=$work_dir
Environment=JAVA_OPTS=
ExecStart=$JAVA_BIN \$JAVA_OPTS -jar $jar_file
SuccessExitStatus=143
Restart=on-failure
RestartSec=5

[Install]
WantedBy=multi-user.target
EOF

  ok "Unit created: $unit_file"
}

HOTEL_JAR="$(resolve_jar "$MOCKS_DIR/mock-hotel-booking-service/target" "mock-hotel-booking-service-*.jar" || true)"
FLIGHT_JAR="$(resolve_jar "$MOCKS_DIR/mock-flight-booking-service/target" "mock-flight-booking-service-*.jar" || true)"
TOURISM_JAR="$(resolve_jar "$MOCKS_DIR/mock-tourist-attractions-service/target" "mock-tourist-attractions-service-*.jar" || true)"
DEMO_JAR="$(resolve_jar "$DEMO_DIR/target" "ns-framework-demo-app-*.jar" || true)"

[ -n "$HOTEL_JAR" ] || err "Jar not found for hotel service. Build first."
[ -n "$FLIGHT_JAR" ] || err "Jar not found for flight service. Build first."
[ -n "$TOURISM_JAR" ] || err "Jar not found for tourism service. Build first."
[ -n "$DEMO_JAR" ] || err "Jar not found for demo service. Build first."

write_unit "mock-hotel-booking-service" "Mock Hotel Booking Service" "$MOCKS_DIR/mock-hotel-booking-service" "$HOTEL_JAR"
write_unit "mock-flight-booking-service" "Mock Flight Booking Service" "$MOCKS_DIR/mock-flight-booking-service" "$FLIGHT_JAR"
write_unit "mock-tourist-attractions-service" "Mock Tourist Attractions Service" "$MOCKS_DIR/mock-tourist-attractions-service" "$TOURISM_JAR"
write_unit "ns-framework-demo" "NS Framework Demo" "$DEMO_DIR" "$DEMO_JAR"

info "Reloading systemd..."
systemctl daemon-reload

info "Enabling services..."
for svc in mock-hotel-booking-service mock-flight-booking-service mock-tourist-attractions-service ns-framework-demo; do
  systemctl enable "$svc"
  ok "Enabled: $svc"
done

if [ "$START_SERVICES" = "true" ]; then
  info "Starting services..."
  for svc in mock-hotel-booking-service mock-flight-booking-service mock-tourist-attractions-service ns-framework-demo; do
    systemctl restart "$svc"
    ok "Started: $svc"
  done
else
  warn "Services were registered and enabled, not started (--start not provided)."
fi

echo ""
echo "Service status summary:"
for svc in mock-hotel-booking-service mock-flight-booking-service mock-tourist-attractions-service ns-framework-demo; do
  enabled="$(systemctl is-enabled "$svc" 2>/dev/null || true)"
  active="$(systemctl is-active "$svc" 2>/dev/null || true)"
  echo "- $svc | enabled=$enabled | active=$active"
done

