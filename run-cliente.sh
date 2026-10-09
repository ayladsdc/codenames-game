#!/bin/bash
# Abre um cliente. Uso: ./run-cliente.sh

# Vai para o diretório onde o script está localizado
cd "$(dirname "$0")"

# Verifica se a pasta out existe
if [ ! -d "out" ]; then
  echo "Pasta out/ nao encontrada. Rode ./build.sh primeiro." >&2
  exit 1
fi

# Executa o cliente (nota: no Linux usamos ":" no lugar de ";" no classpath)
java -cp "out:." cliente.Cliente "$@"