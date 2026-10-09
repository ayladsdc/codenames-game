#!/bin/bash
# Sobe o servidor. Uso: ./run-servidor.sh [porta]   (padrao do projeto: 1996)
cd "$(dirname "$0")"

# Verifica se a pasta out existe
if [ ! -d "out" ]; then
  echo "Pasta out/ nao encontrada. Rode ./build.sh primeiro." >&2
  exit 1
fi

# out = classes compiladas; "." (raiz) = para achar resources/palavras.txt pelo classpath
java -cp "out:." servidor.ServidorCodenames "$@"