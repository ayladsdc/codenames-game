#!/usr/bin/env bash
# Sobe o servidor. Uso: ./run-servidor.sh [porta]   (padrão do projeto: 1996)
#
# Fontes consultadas:
#  - https://docs.oracle.com/en/java/javase/21/docs/specs/man/java.html
#  - https://www.gnu.org/software/bash/manual/bash.html
cd "$(dirname "$0")" || exit 1

if [ ! -d out ]; then
  echo "Pasta out/ não encontrada. Rode ./build.sh primeiro." >&2
  exit 1
fi

# out = classes compiladas; "." (raiz) = para achar resources/palavras.txt pelo classpath
exec java -cp out:. servidor.ServidorCodenames "$@"
