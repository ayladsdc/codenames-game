@echo off
rem Compila o projeto inteiro para out\
rem
rem Fontes consultadas:
rem  - https://docs.oracle.com/en/java/javase/21/docs/specs/man/javac.html
rem  - https://learn.microsoft.com/windows-server/administration/windows-commands/for
rem  - https://learn.microsoft.com/windows-server/administration/windows-commands/setlocal
rem  - https://learn.microsoft.com/windows-server/administration/windows-commands/call
setlocal enabledelayedexpansion

rem trabalha sempre a partir da raiz do repositorio
cd /d "%~dp0"

where javac >nul 2>nul
if errorlevel 1 (
  echo javac nao encontrado. Instale o JDK ^(nao basta o JRE^). 1>&2
  exit /b 1
)

if exist out rmdir /s /q out
if exist fontes.tmp del fontes.tmp

rem Um .java por linha, entre aspas (caminhos com espaco). As barras invertidas viram
rem barras normais porque, dentro de aspas, o javac trata "\" como escape.
for /r src %%f in (*.java) do (
  set "caminho=%%f"
  echo "!caminho:\=/!">>fontes.tmp
)
for /r resources %%f in (*.java) do (
  set "caminho=%%f"
  echo "!caminho:\=/!">>fontes.tmp
)

javac -encoding UTF-8 -d out @fontes.tmp
set "erro=%ERRORLEVEL%"
del fontes.tmp
if not "%erro%"=="0" exit /b %erro%

echo Build ok -^> out\
