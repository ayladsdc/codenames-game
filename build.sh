#!/usr/bin/env bash
# Compila o projeto inteiro para out/.
#
# Fontes consultadas:
#  - https://docs.oracle.com/en/java/javase/21/docs/specs/man/javac.html
#  - https://www.gnu.org/software/bash/manual/html_node/The-Set-Builtin.html
set -e

# trabalha sempre a partir da raiz do repositório (onde o script está)
cd "$(dirname "$0")"

command -v javac >/dev/null 2>&1 || {
  echo "javac não encontrado. Instale o JDK (não basta o JRE)." >&2
  exit 1
}

# lista temporária com um arquivo .java por linha, entre aspas (caminhos com espaço)
lista="$(mktemp)"
trap 'rm -f "$lista"' EXIT
find src resources -name '*.java' | sed 's/.*/"&"/' > "$lista"

rm -rf out
javac -encoding UTF-8 -d out @"$lista"

echo "Build ok -> out/"
