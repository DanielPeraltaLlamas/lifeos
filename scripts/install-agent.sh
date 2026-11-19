#!/usr/bin/env bash
# Instala (o actualiza) el agente de Life OS como servicio de usuario de systemd.
#   ./scripts/install-agent.sh            instalar / actualizar y arrancar
#   ./scripts/install-agent.sh --remove   pararlo y quitarlo
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
DEST="${XDG_DATA_HOME:-$HOME/.local/share}/lifeos/agent"
UNIT="${XDG_CONFIG_HOME:-$HOME/.config}/systemd/user/lifeos-agent.service"
JAVA="${LIFEOS_JAVA:-$HOME/.jdks/jdk-21.0.13+9/bin/java}"
[ -x "$JAVA" ] || JAVA="$(command -v java)"

if [ "${1:-}" = "--remove" ]; then
    systemctl --user disable --now lifeos-agent.service 2>/dev/null || true
    rm -f "$UNIT"
    rm -rf "$DEST"
    systemctl --user daemon-reload
    echo "Agente quitado."
    exit 0
fi

echo "Compilando el agente…"
(cd "$ROOT" && ./gradlew -q :linuxAgent:buildAgent)

echo "Copiando a $DEST"
mkdir -p "$DEST"
rm -rf "$DEST/lib"
cp -r "$ROOT/linuxAgent/build/agent/lib" "$DEST/lib"

mkdir -p "$(dirname "$UNIT")"
cat > "$UNIT" <<UNIT
[Unit]
Description=Life OS: bloqueo durante los bloques de enfoque y sincronización
After=graphical-session.target network-online.target
PartOf=graphical-session.target

[Service]
ExecStart=$JAVA -Xmx96m -XX:+UseSerialGC -Djava.awt.headless=true -cp "$DEST/lib/*" dev.lifeos.agent.MainKt
Restart=always
RestartSec=5

[Install]
WantedBy=graphical-session.target
UNIT

systemctl --user daemon-reload
systemctl --user enable lifeos-agent.service >/dev/null
systemctl --user restart lifeos-agent.service
sleep 3
systemctl --user --no-pager status lifeos-agent.service | head -5
