@echo off
REM Script para iniciar el Servidor RMI de Tarjetas de Crédito
REM El servidor crea automáticamente el RMI Registry

echo ═══════════════════════════════════════════════════════════
echo   SERVIDOR RMI - SISTEMA DE TARJETAS DE CRÉDITO
echo ═══════════════════════════════════════════════════════════
echo.

REM Verificar que build existe
if not exist build\ (
    echo El directorio 'build' no existe
    pause
    exit /b 1
)

REM Verificar que las clases están compiladas
if not exist "build\server\CreditCardServer.class" (
    echo Las clases no están compiladas
    pause
    exit /b 1
)

echo [Verificación] Estructura de directorios correcta
echo.

REM Ejecutar el servidor
REM El servidor crea automáticamente el RMI Registry si no existe
java -cp build server.CreditCardServer

echo.
echo Servidor detenido
pause
