#!/bin/bash
# Empacota o Jotes num instalador nativo com JRE embutido (jpackage do JDK 17+).
#
#   bash package.sh            # instalador para o sistema atual
#   bash package.sh app-image  # só a pasta da aplicação, sem instalador
#
# No Windows produz um .exe/.msi (precisa do WiX Toolset instalado);
# no macOS um .dmg; no Linux um .deb ou .rpm.
set -e

APP_NAME="Jotes"
APP_VERSION="1.0"
MAIN_CLASS="jotes.Main"
BUILD_DIR="build"
DIST_DIR="dist"

case "$(uname -s)" in
  MINGW*|MSYS*|CYGWIN*) SEP=';' ; DEFAULT_TYPE="exe" ;;
  Darwin) SEP=':' ; DEFAULT_TYPE="dmg" ;;
  *) SEP=':' ; DEFAULT_TYPE="deb" ;;
esac
TYPE="${1:-$DEFAULT_TYPE}"

command -v jpackage >/dev/null || {
  echo "jpackage não encontrado. Instala um JDK 17 ou mais recente e põe-no no PATH." >&2
  exit 1
}

echo "==> A compilar (só src/, sem testes)"
rm -rf "$BUILD_DIR"
mkdir -p "$BUILD_DIR/classes" "$BUILD_DIR/libs"
CP=$(ls lib/*/*.jar | tr '\n' "$SEP")
javac -encoding UTF-8 -d "$BUILD_DIR/classes" -cp "$CP" $(find src -name '*.java')
find src -type f ! -name '*.java' -exec cp {} "$BUILD_DIR/classes/" \;

echo "==> A montar o jar da aplicação"
jar --create --file "$BUILD_DIR/libs/jotes.jar" --main-class "$MAIN_CLASS" -C "$BUILD_DIR/classes" .
cp lib/*/*.jar "$BUILD_DIR/libs/"
# JARs de javadoc não têm classes e só engordam o instalador
rm -f "$BUILD_DIR/libs/"*-javadoc.jar "$BUILD_DIR/libs/junit-platform-console-standalone-"*.jar

echo "==> A criar o pacote ($TYPE)"
rm -rf "$DIST_DIR"
mkdir -p "$DIST_DIR"

JPACKAGE_ARGS=(
  --name "$APP_NAME"
  --app-version "$APP_VERSION"
  --input "$BUILD_DIR/libs"
  --main-jar jotes.jar
  --main-class "$MAIN_CLASS"
  --dest "$DIST_DIR"
  --type "$TYPE"
  --description "Jotes — notas em markdown com grafo e canvas"
  --vendor "Jotes"
  --java-options "-Xmx512m"
  --java-options "--enable-native-access=ALL-UNNAMED"
)

# ícone opcional, com a extensão certa para cada sistema
for icon in image/jotes.icns image/jotes.ico image/jotes.png; do
  [ -f "$icon" ] && JPACKAGE_ARGS+=(--icon "$icon") && break
done

if [ "$TYPE" != "app-image" ]; then
  case "$(uname -s)" in
    MINGW*|MSYS*|CYGWIN*) JPACKAGE_ARGS+=(--win-dir-chooser --win-menu --win-shortcut) ;;
    Linux) JPACKAGE_ARGS+=(--linux-shortcut) ;;
  esac
fi

jpackage "${JPACKAGE_ARGS[@]}"
echo "PACOTE OK — ver $DIST_DIR/"
