@echo off
chcp 65001 > nul
REM Script para ejecutar el Cliente GUI RMI de Tarjetas de Crédito

echo ============================================================
echo   CLIENTE GUI RMI - SISTEMA DE TARJETAS DE CREDITO
echo ============================================================
echo.

REM Verificar que build existe
if not exist build\ (
    echo [ERROR] El directorio 'build' no existe.
    echo Ejecute primero .\compile.bat
    pause
    exit /b 1
)

REM Verificar que las clases del cliente GUI estan compiladas
if not exist "build\client\CreditCardClientGUI.class" (
    echo [ERROR] No se encuentra build\client\CreditCardClientGUI.class
    echo Asegurese de haber compilado correctamente.
    pause
    exit /b 1
)

echo [Verificacion] Conectando con servidor en localhost:1070...
echo.

REM Ejecutar el cliente GUI
echo [Iniciando] Cliente grafico...
java -cp build client.CreditCardClientGUI

if errorlevel 1 (
    echo.
    echo [ERROR] Fallo al ejecutar el cliente GUI.
    echo Asegurese de que el servidor RMI este corriendo en otra terminal.
    echo El servidor debe estar en el puerto 1070.
)

echo.
pause