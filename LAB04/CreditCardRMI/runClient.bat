@echo off
REM Script para ejecutar el Cliente RMI de Tarjetas de Crédito (Consola)

echo ═══════════════════════════════════════════════════════════
echo   CLIENTE RMI - SISTEMA DE TARJETAS DE CRÉDITO
echo ═══════════════════════════════════════════════════════════
echo.

REM Verificar que build existe
if not exist build\ (
    echo El directorio 'build' no existe
    pause
    exit /b 1
)

REM Verificar que las clases están compiladas
if not exist "build\client\CreditCardClient.class" (
    echo Las clases no están compiladas
    echo Primero debe ejecutar: compile.bat
    pause
    exit /b 1
)

echo [Verificación] Conectando con servidor en localhost:1098...
echo.

REM Ejecutar el cliente
echo.
pause
