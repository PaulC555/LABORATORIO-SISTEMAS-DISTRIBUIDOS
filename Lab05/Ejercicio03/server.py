"""
server.py — Servidor gRPC del Sistema de Conversión
Conversiones: Temperatura, Moneda, Distancia
"""

import grpc
import logging
import time
from concurrent import futures
from datetime import datetime

import converter_pb2
import converter_pb2_grpc

# ─────────────────────────────────────────────
# Configuración de bienvenida 
# ─────────────────────────────────────────────
LOG_FORMAT = "%(asctime)s  [%(levelname)s]  %(message)s"
logging.basicConfig(
    level=logging.INFO,
    format=LOG_FORMAT,
    datefmt="%Y-%m-%d %H:%M:%S",
    handlers=[
        logging.StreamHandler(),
        logging.FileHandler("server.log", encoding="utf-8"),
    ],
)
log = logging.getLogger("ConverterServer")

# ─────────────────────────────────────────────
# Constantes de conversión
# ─────────────────────────────────────────────
TASA_CAMBIO_SOL_USD = 0.29  
MILLAS_POR_KM       = 0.621371

CONVERSION_META = {
    converter_pb2.CELSIUS_TO_FAHRENHEIT : {
        "from": "°C", "to": "°F",
        "formula": "(°C × 1.8) + 32",
        "min": -273.15, "max": 1_000_000,
    },
    converter_pb2.FAHRENHEIT_TO_CELSIUS : {
        "from": "°F", "to": "°C",
        "formula": "(°F − 32) / 1.8",
        "min": -459.67, "max": 1_800_032,
    },
    converter_pb2.SOLES_TO_DOLARES : {
        "from": "PEN (S/.)", "to": "USD ($)",
        "formula": f"PEN × {TASA_CAMBIO_SOL_USD}",
        "min": 0, "max": 1_000_000_000,
    },
    converter_pb2.DOLARES_TO_SOLES : {
        "from": "USD ($)", "to": "PEN (S/.)",
        "formula": f"USD / {TASA_CAMBIO_SOL_USD}",
        "min": 0, "max": 1_000_000_000,
    },
    converter_pb2.KILOMETROS_TO_MILLAS : {
        "from": "km", "to": "mi",
        "formula": f"km × {MILLAS_POR_KM}",
        "min": 0, "max": 1_000_000_000,
    },
    converter_pb2.MILLAS_TO_KILOMETROS : {
        "from": "mi", "to": "km",
        "formula": f"mi / {MILLAS_POR_KM}",
        "min": 0, "max": 1_000_000_000,
    },
}

# ─────────────────────────────────────────────
# Lógica de conversión
# ─────────────────────────────────────────────
def _do_convert(conv_type: int, value: float) -> float:
    if conv_type == converter_pb2.CELSIUS_TO_FAHRENHEIT:
        return value * 1.8 + 32
    if conv_type == converter_pb2.FAHRENHEIT_TO_CELSIUS:
        return (value - 32) / 1.8
    if conv_type == converter_pb2.SOLES_TO_DOLARES:
        return value * TASA_CAMBIO_SOL_USD
    if conv_type == converter_pb2.DOLARES_TO_SOLES:
        return value / TASA_CAMBIO_SOL_USD
    if conv_type == converter_pb2.KILOMETROS_TO_MILLAS:
        return value * MILLAS_POR_KM
    if conv_type == converter_pb2.MILLAS_TO_KILOMETROS:
        return value / MILLAS_POR_KM
    raise ValueError(f"Tipo de conversión desconocido: {conv_type}")


# ─────────────────────────────────────────────
# Servicer (implementación del servidor gRPC)
# ─────────────────────────────────────────────
class ConverterServicer(converter_pb2_grpc.ConverterServicer):

    def __init__(self):
        self._request_count = 0
        self._start_time    = time.time()
        log.info("ConverterServicer inicializado")

    # ---------- HealthCheck ----------
    def HealthCheck(self, request, context):
        uptime = int(time.time() - self._start_time)
        info   = (
            f"Servidor activo | Uptime: {uptime}s | "
            f"Solicitudes atendidas: {self._request_count}"
        )
        log.info(f"HealthCheck → {info}")
        return converter_pb2.HealthResponse(status="OK", server_info=info)

    # ---------- Convert ----------
    def Convert(self, request, context):
        self._request_count += 1
        ts         = datetime.now().strftime("%H:%M:%S")
        conv_type  = request.type
        value      = request.value
        meta       = CONVERSION_META.get(conv_type)

        log.info(
            f"[{ts}] Solicitud #{self._request_count} → "
            f"tipo={conv_type}, valor={value}"
        )

        # ── Validación: tipo conocido ──
        if meta is None:
            msg = f"Tipo de conversión inválido: {conv_type}"
            log.warning(f"{msg}")
            context.set_code(grpc.StatusCode.INVALID_ARGUMENT)
            context.set_details(msg)
            return converter_pb2.ConvertResponse(
                success=False, error_message=msg
            )

        # ── Validación: rango ──
        if not (meta["min"] <= value <= meta["max"]):
            msg = (
                f"Valor {value} fuera de rango permitido "
                f"[{meta['min']}, {meta['max']}] para {meta['from']} → {meta['to']}"
            )
            log.warning(f"{msg}")
            context.set_code(grpc.StatusCode.OUT_OF_RANGE)
            context.set_details(msg)
            return converter_pb2.ConvertResponse(
                success=False, error_message=msg
            )

        # ── Conversión ──
        try:
            result = _do_convert(conv_type, value)
        except Exception as exc:
            log.error(f"Error en conversión: {exc}")
            context.set_code(grpc.StatusCode.INTERNAL)
            context.set_details(str(exc))
            return converter_pb2.ConvertResponse(
                success=False, error_message=str(exc)
            )

        log.info(
            f"Resultado #{self._request_count}: "
            f"{value} {meta['from']} → {result:.4f} {meta['to']}"
        )

        return converter_pb2.ConvertResponse(
            result       = result,
            unit_from    = meta["from"],
            unit_to      = meta["to"],
            formula      = meta["formula"],
            success      = True,
            error_message= "",
        )


# ─────────────────────────────────────────────
# Arranque del servidor
# ─────────────────────────────────────────────
def serve(port: int = 50051):
    server = grpc.server(futures.ThreadPoolExecutor(max_workers=10))
    converter_pb2_grpc.add_ConverterServicer_to_server(
        ConverterServicer(), server
    )
    address = f"[::]:{port}"
    server.add_insecure_port(address)
    server.start()

    log.info("=" * 55)
    log.info("  Servidor gRPC — Sistema de Conversión")
    log.info(f"  Escuchando en {address}")
    log.info("  Conversiones disponibles:")
    log.info("    • Celsius  ↔  Fahrenheit")
    log.info("    • Soles (PEN)  ↔  Dólares (USD)")
    log.info("    • Kilómetros  ↔  Millas")
    log.info("=" * 55)

    try:
        server.wait_for_termination()
    except KeyboardInterrupt:
        log.info("Servidor detenido por el usuario.")
        server.stop(0)


if __name__ == "__main__":
    serve()
