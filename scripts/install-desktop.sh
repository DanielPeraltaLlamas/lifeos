#!/usr/bin/env bash
# Instala (o actualiza) la app de escritorio de Life OS:
#  - la app empaquetada (con su propio Java) en ~/.local/share/lifeos/app
#  - la entrada del lanzador (~/.local/share/applications/lifeos.desktop) y su icono
#  - el arranque al iniciar Hyprland (bloque marcado en ~/.config/caelestia/hypr-user.lua)
#   ./scripts/install-desktop.sh            instalar / actualizar
#   ./scripts/install-desktop.sh --remove   quitarlo todo
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
DATA="${XDG_DATA_HOME:-$HOME/.local/share}"
DEST="$DATA/lifeos/app"
DESKTOP="$DATA/applications/lifeos.desktop"
ICON="$DATA/icons/hicolor/scalable/apps/lifeos.svg"
ICON_PNG="$DATA/icons/hicolor/256x256/apps/lifeos.png"
HYPR="$HOME/.config/caelestia/hypr-user.lua"
BEGIN="-- >>> Life OS (scripts/install-desktop.sh) >>>"
END="-- <<< Life OS <<<"

remove_autostart() {
    [ -f "$HYPR" ] && sed -i "/^$BEGIN\$/,/^$END\$/d" "$HYPR"
}

if [ "${1:-}" = "--remove" ]; then
    remove_autostart
    rm -rf "$DEST" "$DESKTOP" "$ICON" "$ICON_PNG"
    update-desktop-database "$DATA/applications" 2>/dev/null || true
    echo "Life OS quitado del lanzador y del autostart."
    exit 0
fi

echo "Empaquetando la app…"
(cd "$ROOT" && ./gradlew -q :desktopApp:createDistributable)

echo "Copiando a $DEST"
mkdir -p "$(dirname "$DEST")"
rm -rf "$DEST"
cp -r "$ROOT/desktopApp/build/compose/binaries/main/app/lifeos" "$DEST"

mkdir -p "$(dirname "$ICON")" "$(dirname "$ICON_PNG")" "$(dirname "$DESKTOP")"
cp "$ROOT/desktopApp/icons/lifeos.svg" "$ICON"
cp "$ROOT/desktopApp/icons/lifeos.png" "$ICON_PNG"
cat > "$DESKTOP" <<DESK
[Desktop Entry]
Type=Application
Name=Life OS
GenericName=Planificador personal
Comment=Hoy, calendario, tareas, hábitos y modo enfoque
Exec=$DEST/bin/lifeos
Icon=$ICON_PNG
Terminal=false
Categories=Office;ProjectManagement;Utility;
Keywords=tareas;hábitos;calendario;enfoque;TFG;
StartupWMClass=dev-lifeos-desktop-MainKt
DESK
update-desktop-database "$DATA/applications" 2>/dev/null || true
gtk-update-icon-cache -q "$DATA/icons/hicolor" 2>/dev/null || true

# Autostart en Hyprland (archivo de usuario de Caelestia, que no se pisa al actualizar).
remove_autostart
cat >> "$HYPR" <<LUA
$BEGIN
hl.on("hyprland.start", function()
    hl.exec_cmd("$DEST/bin/lifeos")
end)
$END
LUA

echo "Hecho: Life OS está en el lanzador y se abrirá al iniciar Hyprland."
