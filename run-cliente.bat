@echo off
rem Abre um cliente. Uso: run-cliente.bat [argumentos do cliente]
rem (mesmas fontes do run-servidor.bat)
cd /d "%~dp0"

if not exist out (
  echo Pasta out\ nao encontrada. Rode build.bat primeiro. 1>&2
  exit /b 1
)

java -cp "out;." cliente.ClienteCodenames %*
