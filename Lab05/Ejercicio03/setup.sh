#!/bin/bash
# setup.sh - Instalar dependencias y compilar el .proto

echo "Instalando dependencias..."
pip install grpcio grpcio-tools --break-system-packages -q

echo "Compilando archivo .proto..."
cd /home/claude/grpc_converter
python -m grpc_tools.protoc \
  -I. \
  --python_out=. \
  --grpc_python_out=. \
  converter.proto

echo "Setup completado."
ls -la *.py 2>/dev/null || echo " No se generaron archivos .py"
