@echo off
rem Compila o projeto inteiro para out\
rem
rem Fontes consultadas:
rem  - https://docs.oracle.com/en/java/javase/21/docs/specs/man/javac.html
rem  - https://learn.microsoft.com/windows-server/administration/windows-commands/for
rem  - https://learn.microsoft.com/windows-server/administration/windows-commands/setlocal
rem  - https://learn.microsoft.com/windows-server/administration/windows-commands/call
setlocal enabledelayedexpansion

@echo off

REM Cria a pasta out caso ela não exista
IF NOT EXIST out mkdir out

IF "%1"=="test" (
    echo A compilar o projeto e as classes de teste...
    dir /s /b src\*.java test\*.java > sources.txt
    javac -d out @sources.txt
    del sources.txt
    
    REM Copia a pasta resources para a pasta out de forma silenciosa (>nul)
    xcopy resources out\resources /E /I /Y >nul
    
    echo --- A executar os Testes ---
    java -cp out GeradorTabuleiroTeste
    java -cp out PartidaTeste

) ELSE (
    echo A compilar apenas o codigo fonte principal...
    dir /s /b src\*.java > sources.txt
    javac -d out @sources.txt
    del sources.txt
    
    REM Copia a pasta resources para a pasta out
    xcopy resources out\resources /E /I /Y >nul
)