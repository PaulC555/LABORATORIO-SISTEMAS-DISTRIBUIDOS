"""
=============================================================================
FarmaAndes S.A. - Transacciones Distribuidas
Sistema de Transferencia de Medicamentos entre Sedes
=============================================================================

Caso de Estudio: Transferencia de medicamentos entre almacenes distribuidos
Implementación de atomicidad en transacciones distribuidas con PostgreSQL
"""

import psycopg2
from psycopg2 import sql
import uuid
from datetime import datetime
from enum import Enum
from typing import Optional, Tuple, Dict, List


class EstadoTransaccion(Enum):
    """Estados posibles de una transacción distribuida"""
    PENDIENTE = "PENDIENTE"
    COMPLETADO = "COMPLETADO"
    REVERTIDO = "REVERTIDO"
    ERROR = "ERROR"


class AlmacenFarmaAndes:
    """
    Representa un almacén (nodo) en la red distribuida de FarmaAndes
    Maneja conexiones y transacciones locales
    """

    def __init__(self, nombre_almacen: str, host: str, usuario: str, password: str, db: str):
        """
        Inicializar conexión a un almacén específico
        
        Args:
            nombre_almacen: Nombre del almacén (AREQUIPA, LIMA, CUSCO)
            host: Host de PostgreSQL
            usuario: Usuario PostgreSQL
            password: Contraseña PostgreSQL
            db: Nombre de la base de datos
        """
        self.nombre_almacen = nombre_almacen
        self.host = host
        self.usuario = usuario
        self.password = password
        self.db = db
        self.conexion = None
        self.conectar()

    def conectar(self) -> None:
        """Establecer conexión con la base de datos"""
        try:
            self.conexion = psycopg2.connect(
                host=self.host,
                user=self.usuario,
                password=self.password,
                database=self.db
            )
            print(f"Conectado a {self.nombre_almacen} ({self.db})")
        except psycopg2.Error as e:
            print(f"Error al conectar a {self.nombre_almacen}: {e}")
            raise

    def desconectar(self) -> None:
        """Cerrar conexión"""
        if self.conexion:
            self.conexion.close()
            print(f"Desconectado de {self.nombre_almacen}")

    def verificar_stock(self, producto: str) -> Optional[Dict]:
        """
        Verificar stock disponible de un producto
        
        Args:
            producto: Nombre del producto
            
        Returns:
            Dict con información del producto o None si no existe
        """
        try:
            cursor = self.conexion.cursor()
            query = """
                SELECT id, producto, stock, precio_unitario
                FROM inventario
                WHERE producto = %s
            """
            cursor.execute(query, (producto,))
            resultado = cursor.fetchone()
            cursor.close()

            if resultado:
                return {
                    "id": resultado[0],
                    "producto": resultado[1],
                    "stock": resultado[2],
                    "precio_unitario": resultado[3]
                }
            return None
        except psycopg2.Error as e:
            print(f"Error al verificar stock en {self.nombre_almacen}: {e}")
            return None

    def obtener_stock_actual(self, producto: str) -> int:
        """Obtener cantidad exacta de stock"""
        info = self.verificar_stock(producto)
        return info["stock"] if info else 0

    def descontar_inventario(self, producto: str, cantidad: int) -> bool:
        """
        Descontar cantidad del inventario (TRANSACCIÓN ORIGEN)
        
        Args:
            producto: Producto a descontar
            cantidad: Cantidad a descontar
            
        Returns:
            True si se desconta exitosamente, False en caso contrario
        """
        try:
            cursor = self.conexion.cursor()
            
            # Verificar que hay stock suficiente
            stock_actual = self.obtener_stock_actual(producto)
            if stock_actual < cantidad:
                print(f"Stock insuficiente en {self.nombre_almacen}")
                print(f"Disponible: {stock_actual}, Solicitado: {cantidad}")
                cursor.close()
                return False

            # Actualizar inventario
            query_update = """
                UPDATE inventario
                SET stock = stock - %s,
                    fecha_actualizacion = CURRENT_TIMESTAMP
                WHERE producto = %s
            """
            cursor.execute(query_update, (cantidad, producto))
            
            self.conexion.commit()
            cursor.close()
            print(f"Descuento registrado en {self.nombre_almacen}: {cantidad} unidades de {producto}")
            return True

        except psycopg2.Error as e:
            self.conexion.rollback()
            print(f"Error al descontar en {self.nombre_almacen}: {e}")
            return False

    def incrementar_inventario(self, producto: str, cantidad: int) -> bool:
        """
        Incrementar cantidad en el inventario (TRANSACCIÓN DESTINO)
        
        Args:
            producto: Producto a incrementar
            cantidad: Cantidad a incrementar
            
        Returns:
            True si se incrementa exitosamente
        """
        try:
            cursor = self.conexion.cursor()
            
            stock_anterior = self.obtener_stock_actual(producto)
            
            # Actualizar inventario
            query_update = """
                UPDATE inventario
                SET stock = stock + %s,
                    fecha_actualizacion = CURRENT_TIMESTAMP
                WHERE producto = %s
            """
            cursor.execute(query_update, (cantidad, producto))
            
            self.conexion.commit()
            cursor.close()
            print(f"Incremento registrado en {self.nombre_almacen}: {cantidad} unidades de {producto}")
            return True

        except psycopg2.Error as e:
            self.conexion.rollback()
            print(f"Error al incrementar en {self.nombre_almacen}: {e}")
            return False

    def listar_inventario(self) -> None:
        """Mostrar inventario completo del almacén"""
        try:
            cursor = self.conexion.cursor()
            query = "SELECT id, producto, stock, precio_unitario FROM inventario ORDER BY producto"
            cursor.execute(query)
            resultados = cursor.fetchall()
            cursor.close()

            print(f"\n{'='*70}")
            print(f"INVENTARIO - ALMACÉN {self.nombre_almacen}")
            print(f"{'='*70}")
            print(f"{'ID':<5} {'Producto':<30} {'Stock':<10} {'Precio':<10}")
            print(f"{'-'*70}")
            
            for fila in resultados:
                print(f"{fila[0]:<5} {fila[1]:<30} {fila[2]:<10} S/{fila[3]:<9.2f}")
            
            print(f"{'='*70}\n")

        except psycopg2.Error as e:
            print(f"Error al listar inventario: {e}")


class TransferenciaDistribuida:
    """
    Coordinador de transacciones distribuidas
    Implementa el patrón Two-Phase Commit (2PC)
    """

    def __init__(self, almacen_origen: AlmacenFarmaAndes, almacen_destino: AlmacenFarmaAndes):
        """
        Inicializar coordinador de transferencia
        
        Args:
            almacen_origen: Almacén que envía medicamentos
            almacen_destino: Almacén que recibe medicamentos
        """
        self.almacen_origen = almacen_origen
        self.almacen_destino = almacen_destino
        self.referencia_transaccion = str(uuid.uuid4())
        self.estado = EstadoTransaccion.PENDIENTE

    def transferir_medicamentos(self, producto: str, cantidad: int) -> bool:
        """
        Ejecutar transferencia distribuida de medicamentos
        
        Pasos:
        1. Verificar stock en origen
        2. Phase 1: Preparar transacciones (sin confirmar)
        3. Phase 2: Confirmar ambas transacciones o hacer rollback
        
        Args:
            producto: Producto a transferir
            cantidad: Cantidad a transferir
            
        Returns:
            True si la transferencia fue exitosa
        """
        print(f"\n{'='*70}")
        print(f"INICIANDO TRANSFERENCIA DISTRIBUIDA")
        print(f"{'='*70}")
        print(f"De: {self.almacen_origen.nombre_almacen}")
        print(f"A: {self.almacen_destino.nombre_almacen}")
        print(f"Producto: {producto}")
        print(f"Cantidad: {cantidad}")
        print(f"ID Transacción: {self.referencia_transaccion}")
        print(f"{'-'*70}\n")

        try:
            # FASE 1: VERIFICAR PRECONDICIONES
            print("[FASE 1] Verificando precondiciones...")
            
            info_origen = self.almacen_origen.verificar_stock(producto)
            info_destino = self.almacen_destino.verificar_stock(producto)

            if not info_origen or not info_destino:
                print("Producto no existe en uno o ambos almacenes")
                self.estado = EstadoTransaccion.ERROR
                return False

            if info_origen["stock"] < cantidad:
                print(f"Stock insuficiente en {self.almacen_origen.nombre_almacen}")
                print(f"Disponible: {info_origen['stock']}")
                self.estado = EstadoTransaccion.ERROR
                return False

            print(f"Stock origen: {info_origen['stock']} unidades")
            print(f"Stock destino: {info_destino['stock']} unidades")

            # FASE 2: DESCONTAR EN ORIGEN
            print(f"\n[FASE 2a] Descontando {cantidad} unidades en {self.almacen_origen.nombre_almacen}...")
            if not self.almacen_origen.descontar_inventario(producto, cantidad):
                print(f"Error al descontar. Abortando transacción...")
                self.estado = EstadoTransaccion.REVERTIDO
                return False

            # FASE 2B: INCREMENTAR EN DESTINO
            print(f"\n[FASE 2b] Incrementando {cantidad} unidades en {self.almacen_destino.nombre_almacen}...")
            if not self.almacen_destino.incrementar_inventario(producto, cantidad):
                print(f"Error al incrementar. Revirtiendo cambios...")
                # ROLLBACK manual (la BD ya hizo rollback, pero confirmamos)
                self.estado = EstadoTransaccion.REVERTIDO
                return False

            # FASE 3: CONFIRMACIÓN
            self.estado = EstadoTransaccion.COMPLETADO
            print(f"\nTRANSACCIÓN COMPLETADA EXITOSAMENTE\n")
            return True

        except Exception as e:
            print(f"Error inesperado en transacción distribuida: {e}")
            self.estado = EstadoTransaccion.ERROR
            return False

    def mostrar_resumen(self) -> None:
        """Mostrar resumen de la transacción"""
        print(f"\n{'='*70}")
        print(f"RESUMEN DE TRANSACCIÓN")
        print(f"{'='*70}")
        print(f"Estado: {self.estado.value}")
        print(f"Origen: {self.almacen_origen.nombre_almacen}")
        print(f"Destino: {self.almacen_destino.nombre_almacen}")
        print(f"ID: {self.referencia_transaccion}")
        print(f"{'='*70}\n")


# =============================================================================
# EJERCICIO 2: SIMULACIÓN DE FALLO (NODO LIMA CADE)
# =============================================================================
# =============================================================================

class TransferenciaConFallo:
    """
    Coordinador de transacciones distribuidas con simulación de fallos
    Implementa el patrón Two-Phase Commit con manejo de caídas de nodo
    """

    def __init__(self, almacen_origen: AlmacenFarmaAndes, almacen_destino: AlmacenFarmaAndes):
        """
        Inicializar coordinador con soporte a simulación de fallos
        
        Args:
            almacen_origen: Almacén que envía medicamentos
            almacen_destino: Almacén que recibe medicamentos
        """
        self.almacen_origen = almacen_origen
        self.almacen_destino = almacen_destino
        self.referencia_transaccion = str(uuid.uuid4())
        self.estado = EstadoTransaccion.PENDIENTE
        self.destino_caido = False  # Flag para simular caída

    def marcar_destino_caido(self) -> None:
        """Marca el almacén destino como caído (simula fallo de red)"""
        self.destino_caido = True
        print(f"SIMULANDO CAÍDA: {self.almacen_destino.nombre_almacen} deja de responder")

    def transferir_con_fallo_simulado(self, producto: str, cantidad: int) -> bool:
        """
        Ejecutar transferencia distribuida simulando fallo en FASE 2b
        
        Fases:
        1. Verificar precondiciones
        2a. Descontar en origen
        2b. FALLO: Destino no responde
        3. ROLLBACK: Revertir todos los cambios
        
        Args:
            producto: Producto a transferir
            cantidad: Cantidad a transferir
            
        Returns:
            False (transacción falla y se revierte)
        """
        print(f"\n{'='*70}")
        print(f"INICIANDO TRANSFERENCIA DISTRIBUIDA")
        print(f"{'='*70}")
        print(f"De: {self.almacen_origen.nombre_almacen}")
        print(f"A: {self.almacen_destino.nombre_almacen}")
        print(f"Producto: {producto}")
        print(f"Cantidad: {cantidad}")
        print(f"ID Transacción: {self.referencia_transaccion}")
        print(f"{'-'*70}\n")

        try:
            # FASE 1: VERIFICAR PRECONDICIONES
            print("[FASE 1] Verificando precondiciones...")
            
            info_origen = self.almacen_origen.verificar_stock(producto)
            info_destino = self.almacen_destino.verificar_stock(producto)

            if not info_origen or not info_destino:
                print("Producto no existe en uno o ambos almacenes")
                self.estado = EstadoTransaccion.ERROR
                return False

            if info_origen["stock"] < cantidad:
                print(f"Stock insuficiente en {self.almacen_origen.nombre_almacen}")
                self.estado = EstadoTransaccion.ERROR
                return False

            stock_origen_inicial = info_origen["stock"]
            stock_destino_inicial = info_destino["stock"]
            
            print(f"Stock origen inicial: {stock_origen_inicial} unidades")
            print(f"Stock destino inicial: {stock_destino_inicial} unidades")

            # FASE 2a: DESCONTAR EN ORIGEN
            print(f"\n[FASE 2a] Descontando {cantidad} unidades en {self.almacen_origen.nombre_almacen}...")
            
            if not self.almacen_origen.descontar_inventario(producto, cantidad):
                print(f"Fallo al descontar. Abortando...")
                self.estado = EstadoTransaccion.ERROR
                return False

            stock_origen_despues = self.almacen_origen.obtener_stock_actual(producto)
            print(f"Stock en {self.almacen_origen.nombre_almacen} actualizado: {stock_origen_inicial} → {stock_origen_despues}")

            # FASE 2b: INCREMENTAR EN DESTINO (AQUÍ OCURRE EL FALLO)
            print(f"\n[FASE 2b] Intentando incrementar {cantidad} unidades en {self.almacen_destino.nombre_almacen}...")
            
            if self.destino_caido:
                print(f"{self.almacen_destino.nombre_almacen} NO RESPONDE (conexión perdida)")
                print(f"FALLO en FASE 2b: Operación en destino rechazada")
                
                # FASE 3: ROLLBACK (REVERTIR CAMBIOS EN ORIGEN)
                print(f"\n[FASE 3] Ejecutando ROLLBACK automático...")
                print(f"Revirtiendo cambios en {self.almacen_origen.nombre_almacen}...")
                
                # Incrementar nuevamente en origen (revertir descuento)
                if self.almacen_origen.incrementar_inventario(producto, cantidad):
                    stock_revertido = self.almacen_origen.obtener_stock_actual(producto)
                    print(f"Rollback completado en {self.almacen_origen.nombre_almacen}")
                    print(f"Stock revertido a: {stock_revertido} unidades (original: {stock_origen_inicial})")
                    
                    self.estado = EstadoTransaccion.REVERTIDO
                    return False
                else:
                    print(f"Error crítico: No se pudo revertir el cambio en {self.almacen_origen.nombre_almacen}")
                    self.estado = EstadoTransaccion.ERROR
                    return False
            
            # Si no está caído, continuar normalmente
            if not self.almacen_destino.incrementar_inventario(producto, cantidad):
                print(f"Fallo al incrementar. Ejecutando rollback...")
                self.almacen_origen.incrementar_inventario(producto, cantidad)
                self.estado = EstadoTransaccion.REVERTIDO
                return False

            self.estado = EstadoTransaccion.COMPLETADO
            print(f"\nTRANSACCIÓN COMPLETADA EXITOSAMENTE")
            return True

        except Exception as e:
            print(f"Error inesperado: {e}")
            self.estado = EstadoTransaccion.ERROR
            return False

    def mostrar_resumen(self) -> None:
        """Mostrar resumen de la transacción"""
        print(f"\n{'='*70}")
        print(f"RESUMEN DE TRANSACCIÓN")
        print(f"{'='*70}")
        print(f"Estado: {self.estado.value}")
        print(f"Origen: {self.almacen_origen.nombre_almacen}")
        print(f"Destino: {self.almacen_destino.nombre_almacen}")
        print(f"ID: {self.referencia_transaccion}")
        
        if self.estado == EstadoTransaccion.REVERTIDO:
            print(f"\nTRANSACCIÓN REVERTIDA")
            print(f"   Motivo: Nodo destino no respondió")
            print(f"   Acción: Rollback automático ejecutado")
            print(f"   Resultado: Todos los cambios revirtieron")
        elif self.estado == EstadoTransaccion.ERROR:
            print(f"\nTRANSACCIÓN ERROR")
        else:
            print(f"\nTRANSACCIÓN COMPLETADA")
        
        print(f"{'='*70}\n")


def ejercicio_1_transferencia_exitosa():
    """
    EJERCICIO 1: Transferencia Exitosa
    
    Escenario:
    - Transferir 20 unidades de Paracetamol desde Arequipa a Lima
    - Verificar atomicidad: ambas operaciones se completan
    - Mostrar estado inicial y final
    """
    
    print("\n\n")
    print("█" * 70)
    print("█" + " " * 68 + "█")
    print("█" + "  EJERCICIO 1: TRANSFERENCIA EXITOSA".center(68) + "█")
    print("█" + " " * 68 + "█")
    print("█" * 70)
    
    CONFIG = {
        "host": "localhost",
        "usuario": "postgres",
        "password": "postgres",
    }

    try:
        print("\n" + "="*70)
        print("FARMAANDES S.A. - CASO DE ESTUDIO")
        print("Transacciones Distribuidas en Sistema de Inventario")
        print("="*70 + "\n")

        print("[1] Conectando a almacenes...\n")
        
        almacen_arequipa = AlmacenFarmaAndes(
            "AREQUIPA",
            CONFIG["host"],
            CONFIG["usuario"],
            CONFIG["password"],
            "almacen_arequipa"
        )

        almacen_lima = AlmacenFarmaAndes(
            "LIMA",
            CONFIG["host"],
            CONFIG["usuario"],
            CONFIG["password"],
            "almacen_lima"
        )

        print("\n[2] Estado inicial de inventario:\n")
        almacen_arequipa.listar_inventario()
        almacen_lima.listar_inventario()

        print("\n[3] Ejecutando Ejercicio 1: Transferencia Exitosa\n")
        print("Transferir 20 unidades de Paracetamol 500mg")
        print("Desde Arequipa hacia Lima\n")

        transferencia = TransferenciaDistribuida(almacen_arequipa, almacen_lima)
        exito = transferencia.transferir_medicamentos("Paracetamol 500mg", 20)

        print("[4] Estado final de inventario:\n")
        almacen_arequipa.listar_inventario()
        almacen_lima.listar_inventario()

        transferencia.mostrar_resumen()

        if exito:
            print("Ejercicio 1: COMPLETADO")
            print("Arequipa: 100 → 80 unidades")
            print("Lima: 50 → 70 unidades")
        else:
            print("Ejercicio 1: FALLÓ")

        almacen_arequipa.desconectar()
        almacen_lima.desconectar()

    except Exception as e:
        print(f"Error fatal: {e}")


def ejercicio_2_simulacion_fallo():
    """
    EJERCICIO 2: Simulación de Fallo en Nodo Destino
    
    Escenario:
    - Iniciar transacción para transferir 30 unidades de Paracetamol
    - Descontar exitosamente en Arequipa
    - Simular que Lima deja de responder
    - Ejecutar rollback automático
    - Verificar que ambos almacenes quedan en estado inicial
    """
    
    print("\n\n")
    print("█" * 70)
    print("█" + " " * 68 + "█")
    print("█" + "  EJERCICIO 2: SIMULACIÓN DE FALLO EN NODO DESTINO".center(68) + "█")
    print("█" + " " * 68 + "█")
    print("█" * 70)
    print("")
    
    # Configuración
    CONFIG = {
        "host": "localhost",
        "usuario": "postgres",
        "password": "postgres",
    }

    try:
        # Conectar a almacenes
        print("[1] Conectando a almacenes...\n")
        
        almacen_arequipa = AlmacenFarmaAndes(
            "AREQUIPA",
            CONFIG["host"],
            CONFIG["usuario"],
            CONFIG["password"],
            "almacen_arequipa"
        )

        almacen_lima = AlmacenFarmaAndes(
            "LIMA",
            CONFIG["host"],
            CONFIG["usuario"],
            CONFIG["password"],
            "almacen_lima"
        )

        # Estado inicial
        print("\n[2] Estado inicial de inventario:\n")
        print(f"Almacén AREQUIPA:")
        stock_aq_inicial = almacen_arequipa.obtener_stock_actual("Paracetamol 500mg")
        print(f"  Paracetamol 500mg: {stock_aq_inicial} unidades")
        
        print(f"\nAlmacén LIMA:")
        stock_lima_inicial = almacen_lima.obtener_stock_actual("Paracetamol 500mg")
        print(f"  Paracetamol 500mg: {stock_lima_inicial} unidades")

        # Crear transacción
        print("\n[3] Creando transacción distribuida...\n")
        transferencia_fallo = TransferenciaConFallo(almacen_arequipa, almacen_lima)
        
        # SIMULAR CAÍDA DE LIMA
        print("[4] Simulando caída de red hacia Lima...\n")
        transferencia_fallo.marcar_destino_caido()

        # Ejecutar transferencia (fallará)
        print("\n[5] Ejecutando transferencia (30 unidades)...\n")
        exito = transferencia_fallo.transferir_con_fallo_simulado("Paracetamol 500mg", 30)

        # Mostrar resumen
        print("\n[6] Resumen de transacción:\n")
        transferencia_fallo.mostrar_resumen()

        # Verificar estado final (debe ser igual al inicial)
        print("[7] Verificando estado final después del rollback:\n")
        
        stock_aq_final = almacen_arequipa.obtener_stock_actual("Paracetamol 500mg")
        stock_lima_final = almacen_lima.obtener_stock_actual("Paracetamol 500mg")
        
        print(f"Almacén AREQUIPA:")
        print(f"  Paracetamol 500mg: {stock_aq_final} unidades (inicial: {stock_aq_inicial})")
        if stock_aq_final == stock_aq_inicial:
            print(f"Stock revertido correctamente")
        else:
            print(f"ERROR: Stock no coincide")
        
        print(f"\nAlmacén LIMA:")
        print(f"  Paracetamol 500mg: {stock_lima_final} unidades (inicial: {stock_lima_inicial})")
        if stock_lima_final == stock_lima_inicial:
            print(f"Stock sin cambios (como debe ser)")
        else:
            print(f"ERROR: Stock cambió sin permiso")

        # Resultado final
        print(f"\n{'='*70}")
        print("RESULTADO EJERCICIO 2")
        print(f"{'='*70}")
        
        if (stock_aq_final == stock_aq_inicial and 
            stock_lima_final == stock_lima_inicial and 
            transferencia_fallo.estado == EstadoTransaccion.REVERTIDO):
            print("\nEJERCICIO 2 COMPLETADO: Rollback automático funcionó correctamente")
            print("  1. Descuento registrado en Arequipa (temporalmente)")
            print("  2. Caída de Lima detectada")
            print("  3. Rollback automático ejecutado")
            print("  4. Stock de Arequipa revertido a su valor inicial")
            print("  5. Stock de Lima sin cambios")
        else:
            print("\nEJERCICIO 2 FALLÓ")
            print(f"  Estado final: {transferencia_fallo.estado.value}")
        
        print(f"\n{'='*70}\n")

        # Desconectar
        almacen_arequipa.desconectar()
        almacen_lima.desconectar()

    except Exception as e:
        print(f"Error: {e}")


# =============================================================================
# SELECTOR DE EJERCICIOS
# =============================================================================

def menu_principal():
    """
    Menú interactivo para seleccionar qué ejercicio ejecutar
    """
    print("\n" + "=" * 70)
    print("FARMAANDES S.A. - LABORATORIO 08")
    print("Transacciones Distribuidas - Selector de Ejercicios")
    print("=" * 70 + "\n")
    
    print("Ejercicios disponibles:")
    print("\n1 - Ejercicio 1: Transferencia Exitosa")
    print("    - Transferir 20 unidades sin problemas")
    print("    - Verificar: Arequipa 100->80, Lima 50->70")
    print("\n2 - Ejercicio 2: Simulacion de Fallo (Nodo Lima cae)")
    print("    - Descontar en Arequipa exitosamente")
    print("    - Lima no responde -> Rollback automatico")
    print("    - Verificar: Todos los cambios revertidos")
    print("\n0 - Ejecutar ambos ejercicios")
    print("\n" + "=" * 70 + "\n")
    
    opcion = input("Selecciona un ejercicio (0-2): ").strip()
    return opcion


if __name__ == "__main__":
    opcion = menu_principal()
    
    if opcion == "1":
        # Solo Ejercicio 1
        ejercicio_1_transferencia_exitosa()
    
    elif opcion == "2":
        # Solo Ejercicio 2
        ejercicio_2_simulacion_fallo()
    
    elif opcion == "0":
        # Ejecutar ambos ejercicios
        ejercicio_1_transferencia_exitosa()
        
        print("\n\nEsperando 2 segundos antes del Ejercicio 2...\n")
        import time
        time.sleep(2)
        
        ejercicio_2_simulacion_fallo()
    
    else:
        print("Opción inválida")
