@echo off
rem Sobe o servidor. Uso: run-servidor.bat [porta]   (padrao do projeto: 1996)
rem
rem Fontes consultadas:
rem  - https://docs.oracle.com/en/java/javase/21/docs/specs/man/java.html
rem  - https://learn.microsoft.com/windows-server/administration/windows-commands/call
cd /d "%~dp0"

if not exist out (
  echo Pasta out\ nao encontrada. Rode build.bat primeiro. 1>&2
  exit /b 1
)

rem out = classes compiladas; "." (raiz) = para achar resources\palavras.txt pelo classpath
java -cp "out;." servidor.ServidorCodenames %*
