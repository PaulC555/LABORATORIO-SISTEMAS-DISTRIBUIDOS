@echo off
REM ============================================================
REM Script de compilación para el sistema RMI de Tarjetas de Crédito
REM Compila todas las clases Java y las coloca en el directorio build/
REM ============================================================

echo ═══════════════════════════════════════════════════════════
echo   COMPILANDO SISTEMA RMI - TARJETAS DE CRÉDITO
echo ═══════════════════════════════════════════════════════════
echo.

REM Crear directorio build si no existe
if not exist build (
    echo [Creando] Directorio build...
    mkdir build
    echo.
)

REM Compilar todas las clases Java
echo [Compilando] Clases del paquete remote...
javac -d build -cp build src/remote/*.java
if errorlevel 1 (
    echo [ERROR] Fallo en compilacion de remote.
    pause
    exit /b 1
)

echo [Compilando] Clases del paquete impl...
javac -d build -cp build src/impl/*.java
if errorlevel 1 (
    echo [ERROR] Fallo en compilacion de impl.
    pause
    exit /b 1
)

echo [Compilando] Clases del paquete server...
javac -d build -cp build src/server/*.java
if errorlevel 1 (
    echo [ERROR] Fallo en compilacion de server.
    pause
    exit /b 1
)

echo [Compilando] Clases del paquete client...
javac -d build -cp build src/client/*.java
if errorlevel 1 (
    echo [ERROR] Fallo en compilacion de client.
    pause
    exit /b 1
)

echo.
echo ═══════════════════════════════════════════════════════════
echo   COMPILACION COMPLETADA EXITOSAMENTE
echo ═══════════════════════════════════════════════════════════
echo.
echo Para ejecutar el servidor:   runServer.bat
echo Para ejecutar cliente consola: runClient.bat
echo Para ejecutar cliente GUI:    runClientGUI.bat
echo.

pause