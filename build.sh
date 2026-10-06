#!/usr/bin/env bash
# Compila o projeto inteiro para out/.
#
# Fontes consultadas:
#  - https://docs.oracle.com/en/java/javase/21/docs/specs/man/javac.html
#  - https://www.gnu.org/software/bash/manual/html_node/The-Set-Builtin.html
set -e

# trabalha sempre a partir da raiz do repositório (onde o script está)

mkdir -p out

if [ "$1" = "test" ]; then
    echo "A compilar o projeto e as classes de teste..."
    javac -d out $(find src test -name "*.java")
    
    # Copia a pasta resources para a pasta out
    cp -r resources out/
    
    echo "--- A executar os Testes ---"
    java -cp out GeradorTabuleiroTeste
    java -cp out PartidaTeste
else
    echo "A compilar apenas o código fonte principal..."
    javac -d out $(find src -name "*.java")
    
    # Copia a pasta resources para a pasta out
    cp -r resources out/
fi