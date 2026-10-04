#!/usr/bin/env bash
# Abre um cliente. Uso: ./run-cliente.sh [argumentos do cliente]
# (mesmas fontes do run-servidor.sh)
cd "$(dirname "$0")" || exit 1

if [ ! -d out ]; then
  echo "Pasta out/ não encontrada. Rode ./build.sh primeiro." >&2
  exit 1
fi

exec java -cp out:. cliente.ClienteCodenames "$@"
