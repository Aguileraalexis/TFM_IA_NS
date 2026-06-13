#!/usr/bin/env bash
set -e

[ "$(id -u)" -eq 0 ] || { echo "Ejecuta como root: sudo sh register-systemd-services.sh"; exit 1; }
[ -d "$(pwd)/ns-mock-services" ] || { echo "Debes ejecutarlo desde la raiz del proyecto (carpeta que contiene ns-mock-services y ns-framework-demo)"; exit 1; }
[ -d "$(pwd)/ns-framework-demo" ] || { echo "Debes ejecutarlo desde la raiz del proyecto (carpeta que contiene ns-mock-services y ns-framework-demo)"; exit 1; }

ROOT_DIR="$(pwd)"

# mock-hotel-booking-service
printf '%s\n' '[Unit]' 'Description=Mock Hotel Booking Service' 'After=network.target' '' '[Service]' 'Type=simple' 'User=root' "WorkingDirectory=${ROOT_DIR}/ns-mock-services/mock-hotel-booking-service" "ExecStart=/usr/bin/java -jar ${ROOT_DIR}/ns-mock-services/mock-hotel-booking-service/target/mock-hotel-booking-service-0.1.0.jar" 'SuccessExitStatus=143' 'Restart=on-failure' 'RestartSec=5' '' '[Install]' 'WantedBy=multi-user.target' > /etc/systemd/system/mock-hotel-booking-service.service
systemctl daemon-reload
systemctl disable mock-hotel-booking-service >/dev/null 2>&1 || true
echo "[OK] mock-hotel-booking-service registrado"

# mock-flight-booking-service
printf '%s\n' '[Unit]' 'Description=Mock Flight Booking Service' 'After=network.target' '' '[Service]' 'Type=simple' 'User=root' "WorkingDirectory=${ROOT_DIR}/ns-mock-services/mock-flight-booking-service" "ExecStart=/usr/bin/java -jar ${ROOT_DIR}/ns-mock-services/mock-flight-booking-service/target/mock-flight-booking-service-0.1.0.jar" 'SuccessExitStatus=143' 'Restart=on-failure' 'RestartSec=5' '' '[Install]' 'WantedBy=multi-user.target' > /etc/systemd/system/mock-flight-booking-service.service
systemctl daemon-reload
systemctl disable mock-flight-booking-service >/dev/null 2>&1 || true
echo "[OK] mock-flight-booking-service registrado"

# mock-tourist-attractions-service
printf '%s\n' '[Unit]' 'Description=Mock Tourist Attractions Service' 'After=network.target' '' '[Service]' 'Type=simple' 'User=root' "WorkingDirectory=${ROOT_DIR}/ns-mock-services/mock-tourist-attractions-service" "ExecStart=/usr/bin/java -jar ${ROOT_DIR}/ns-mock-services/mock-tourist-attractions-service/target/mock-tourist-attractions-service-0.1.0.jar" 'SuccessExitStatus=143' 'Restart=on-failure' 'RestartSec=5' '' '[Install]' 'WantedBy=multi-user.target' > /etc/systemd/system/mock-tourist-attractions-service.service
systemctl daemon-reload
systemctl disable mock-tourist-attractions-service >/dev/null 2>&1 || true
echo "[OK] mock-tourist-attractions-service registrado"

# ns-framework-demo
printf '%s\n' '[Unit]' 'Description=NS Framework Demo' 'After=network.target' '' '[Service]' 'Type=simple' 'User=root' "WorkingDirectory=${ROOT_DIR}/ns-framework-demo" "ExecStart=/usr/bin/java -jar ${ROOT_DIR}/ns-framework-demo/target/ns-framework-demo-app-0.1.0-SNAPSHOT.jar" 'SuccessExitStatus=143' 'Restart=on-failure' 'RestartSec=5' '' '[Install]' 'WantedBy=multi-user.target' > /etc/systemd/system/ns-framework-demo.service
systemctl daemon-reload
systemctl disable ns-framework-demo >/dev/null 2>&1 || true
echo "[OK] ns-framework-demo registrado"

echo "Listo: los 4 servicios existen en systemd y quedan deshabilitados (sin autostart)."
