#!/bin/bash
# Compila todo o projeto (src + test) para bin/ e copia os recursos. Uso: bash build.sh
set -e
# limpa para não ficarem classes de ficheiros entretanto renomeados ou apagados
rm -rf bin
mkdir -p bin
# separador do classpath: ';' no Windows (Git Bash/MSYS), ':' no resto
case "$(uname -s)" in
  MINGW*|MSYS*|CYGWIN*) SEP=';' ;;
  *) SEP=':' ;;
esac
CP=$(ls lib/*/*.jar | tr '\n' "$SEP")
SOURCES=$(find src -name '*.java')
if [ -d test ]; then
  SOURCES="$SOURCES $(find test -name '*.java' 2>/dev/null)"
fi
javac -encoding UTF-8 -d bin -cp "$CP" $SOURCES
# recursos (logback.xml e afins) têm de ir para o classpath de execução
find src -type f ! -name '*.java' -exec cp {} bin/ \;
echo "BUILD OK"
