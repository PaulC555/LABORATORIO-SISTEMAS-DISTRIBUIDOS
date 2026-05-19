"""
client.py — Cliente gRPC 
Sistema de Conversión Distribuido
"""

import grpc
import sys
import time

import converter_pb2
import converter_pb2_grpc

# ─────────────────────────────────────────────
# Paleta de colores ANSI
# ─────────────────────────────────────────────
class C:
    RESET  = "\033[0m"
    BOLD   = "\033[1m"
    CYAN   = "\033[96m"
    GREEN  = "\033[92m"
    YELLOW = "\033[93m"
    RED    = "\033[91m"
    BLUE   = "\033[94m"
    MAGENTA= "\033[95m"
    WHITE  = "\033[97m"
    DIM    = "\033[2m"
    BG_DARK= "\033[40m"

# ─────────────────────────────────────────────
# Helpers de UI
# ─────────────────────────────────────────────
WIDTH = 58

def line(char="─"):
    return char * WIDTH

def header():
    print()
    print(f"{C.CYAN}{C.BOLD}╔{'═'*(WIDTH-2)}╗{C.RESET}")
    print(f"{C.CYAN}{C.BOLD}║{'Sistema de Conversión gRPC — Python':^{WIDTH-2}}║{C.RESET}")
    print(f"{C.CYAN}{C.BOLD}╚{'═'*(WIDTH-2)}╝{C.RESET}")
    print()

def section(title: str, emoji: str = ""):
    print(f"\n{C.YELLOW}{C.BOLD}  {emoji}  {title}{C.RESET}")
    print(f"  {C.DIM}{line()}{C.RESET}")

def result_box(value_in, unit_from, result, unit_to, formula):
    print(f"\n  {C.GREEN}┌{'─'*40}┐{C.RESET}")
    print(f"  {C.GREEN}│{C.RESET}  {'Entrada:':10} {C.WHITE}{C.BOLD}{value_in:>10.4f}{C.RESET} {C.CYAN}{unit_from:<12}{C.GREEN}│{C.RESET}")
    print(f"  {C.GREEN}│{C.RESET}  {'Resultado:':10} {C.GREEN}{C.BOLD}{result:>10.4f}{C.RESET} {C.CYAN}{unit_to:<12}{C.GREEN}│{C.RESET}")
    print(f"  {C.GREEN}│{C.RESET}  {C.DIM}Fórmula: {formula:<30}{C.GREEN}│{C.RESET}")
    print(f"  {C.GREEN}└{'─'*40}┘{C.RESET}")

def error_box(msg: str):
    print(f"\n  {C.RED}┌{'─'*40}┐{C.RESET}")
    print(f"  {C.RED}│  ❌  {'ERROR':<36}│{C.RESET}")
    print(f"  {C.RED}│  {msg[:38]:<38}│{C.RESET}")
    print(f"  {C.RED}└{'─'*40}┘{C.RESET}")

# ─────────────────────────────────────────────
# Menú de conversiones
# ─────────────────────────────────────────────
MENU_OPTIONS = [
    (1, converter_pb2.CELSIUS_TO_FAHRENHEIT,  "Celsius → Fahrenheit",   "🌡️ ", "°C"),
    (2, converter_pb2.FAHRENHEIT_TO_CELSIUS,  "Fahrenheit → Celsius",   "🌡️ ", "°F"),
    (3, converter_pb2.SOLES_TO_DOLARES,       "Soles (PEN) → Dólares",  "💰", "S/."),
    (4, converter_pb2.DOLARES_TO_SOLES,       "Dólares → Soles (PEN)",  "💵", "USD"),
    (5, converter_pb2.KILOMETROS_TO_MILLAS,   "Kilómetros → Millas",    "📏", "km"),
    (6, converter_pb2.MILLAS_TO_KILOMETROS,   "Millas → Kilómetros",    "📐", "mi"),
]

def show_menu():
    section("Conversiones disponibles", "🔄")
    for num, _, label, emoji, unit in MENU_OPTIONS:
        print(f"  {C.YELLOW}[{num}]{C.RESET}  {emoji}  {label}  {C.DIM}({unit}){C.RESET}")
    print(f"  {C.YELLOW}[7]{C.RESET}  🏥  Estado del servidor")
    print(f"  {C.YELLOW}[0]{C.RESET}  🚪  Salir")

# ─────────────────────────────────────────────
# Lógica de cliente
# ─────────────────────────────────────────────
def get_float_input(prompt: str) -> float | None:
    raw = input(f"\n  {C.CYAN}➤  {prompt}: {C.RESET}").strip()
    try:
        return float(raw)
    except ValueError:
        error_box(f"'{raw}' no es un número válido.")
        return None

def run_conversion(stub, conv_type, unit_label):
    value = get_float_input(f"Ingresa el valor en {unit_label}")
    if value is None:
        return

    print(f"\n  {C.DIM}⏳ Enviando solicitud al servidor...{C.RESET}")
    try:
        req      = converter_pb2.ConvertRequest(type=conv_type, value=value)
        t0       = time.perf_counter()
        response = stub.Convert(req, timeout=5)
        elapsed  = (time.perf_counter() - t0) * 1000

        if response.success:
            result_box(
                value, response.unit_from,
                response.result, response.unit_to,
                response.formula,
            )
            print(f"  {C.DIM}⚡ Tiempo de respuesta: {elapsed:.1f} ms{C.RESET}")
        else:
            error_box(response.error_message)

    except grpc.RpcError as e:
        error_box(f"gRPC [{e.code().name}]: {e.details()}")

def health_check(stub):
    print(f"\n  {C.DIM}🔍 Consultando estado del servidor...{C.RESET}")
    try:
        resp = stub.HealthCheck(converter_pb2.HealthRequest(), timeout=3)
        print(f"\n  {C.GREEN}● Estado: {resp.status}{C.RESET}")
        print(f"  {C.DIM}{resp.server_info}{C.RESET}")
    except grpc.RpcError as e:
        error_box(f"Servidor no responde: {e.details()}")

# ─────────────────────────────────────────────
# Bucle principal
# ─────────────────────────────────────────────
def main(host: str = "localhost", port: int = 50051):
    address = f"{host}:{port}"
    channel = grpc.insecure_channel(address)
    stub    = converter_pb2_grpc.ConverterStub(channel)

    header()
    print(f"  {C.DIM}Conectado a: {address}{C.RESET}")

    while True:
        show_menu()
        choice = input(f"\n  {C.CYAN}➤  Elige una opción [0-7]: {C.RESET}").strip()

        if choice == "0":
            print(f"\n  {C.YELLOW}👋 ¡Hasta luego!{C.RESET}\n")
            break
        elif choice == "7":
            health_check(stub)
        else:
            found = next((opt for opt in MENU_OPTIONS if str(opt[0]) == choice), None)
            if found:
                _, conv_type, label, emoji, unit = found
                section(label, emoji)
                run_conversion(stub, conv_type, unit)
            else:
                error_box("Opción inválida. Elige entre 0 y 7.")

    channel.close()


if __name__ == "__main__":
    host = sys.argv[1] if len(sys.argv) > 1 else "localhost"
    port = int(sys.argv[2]) if len(sys.argv) > 2 else 50051
    main(host, port)
