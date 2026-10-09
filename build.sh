#!/bin/bash
# Compila o projeto inteiro para out/
# Cria a pasta out caso ela não exista

mkdir -p out

if [ "$1" = "test" ]; then
    echo "A compilar o projeto e as classes de teste..."
    
    # Procura todos os ficheiros .java no src e no test
    find src test -name "*.java" > sources.txt
    javac -d out @sources.txt
    rm sources.txt
    
    # Copia a pasta resources para a pasta out
    cp -r resources out/ 2>/dev/null
    
    echo "--- A executar os Testes ---"
    java -cp out GeradorTabuleiroTeste
    java -cp out PartidaTeste

else
    echo "A compilar apenas o codigo fonte principal..."
    
    # Procura todos os ficheiros .java apenas no src
    find src -name "*.java" > sources.txt
    javac -d out @sources.txt
    rm sources.txt
    
    # Copia a pasta resources para a pasta out
    cp -r resources out/ 2>/dev/null
fi