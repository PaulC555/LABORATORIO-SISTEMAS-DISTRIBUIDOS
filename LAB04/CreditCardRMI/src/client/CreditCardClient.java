package client;

import remote.CreditCardRemote;
import remote.TransactionRecord;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Scanner;

/**
 * Cliente RMI para acceder al servicio de tarjetas de crédito.
 * 
 * Este cliente proporciona una interfaz de línea de comandos para:
 * 1. Crear cuentas de tarjeta de crédito
 * 2. Realizar cargos (compras)
 * 3. Realizar pagos
 * 4. Consultar saldo
 * 5. Ver historial de transacciones
 * 6. Obtener información de la cuenta
 */
public class CreditCardClient {
    
    private static final String NOMBRE_SERVICIO = "CreditCardService";
    private static final int PUERTO_RMI = 1070;
    private static final String HOST = "localhost";
    
    private CreditCardRemote servicio;
    private Scanner scanner;
    
    /**
     * Constructor que inicializa la conexión con el servidor RMI.
     * @throws RemoteException si hay error de conexión
     * @throws NotBoundException si el servicio no está registrado
     */
    public CreditCardClient() throws RemoteException, NotBoundException {
        // Conectar al RMI Registry
        System.out.println("[Cliente] Conectando a RMI Registry en " + HOST + ":" + PUERTO_RMI + "...");
        Registry registry = LocateRegistry.getRegistry(HOST, PUERTO_RMI);
        
        // Buscar el servicio
        System.out.println("[Cliente] Buscando servicio '" + NOMBRE_SERVICIO + "'...");
        servicio = (CreditCardRemote) registry.lookup(NOMBRE_SERVICIO);
        System.out.println("[OK] Conexión establecida correctamente\n");
        
        this.scanner = new Scanner(System.in);
    }
    
    /**
     * Inicia el menú interactivo del cliente.
     */
    public void iniciar() {
        boolean ejecutando = true;
        
        while (ejecutando) {
            mostrarMenu();
            System.out.print("Seleccione opción: ");
            String opcion = scanner.nextLine().trim();
            
            try {
                switch (opcion) {
                    case "1":
                        crearCuenta();
                        break;
                    case "2":
                        realizarCargo();
                        break;
                    case "3":
                        realizarPago();
                        break;
                    case "4":
                        consultarSaldo();
                        break;
                    case "5":
                        verHistorial();
                        break;
                    case "6":
                        mostrarInfoCuenta();
                        break;
                    case "0":
                        ejecutando = false;
                        System.out.println("\nSesión finalizada. ¡Hasta luego!");
                        break;
                    default:
                        System.out.println("Opción no válida. Intente nuevamente.\n");
                }
            } catch (IllegalArgumentException e) {
                System.err.println("Error de validación: " + e.getMessage() + "\n");
            } catch (RemoteException e) {
                System.err.println("Error de comunicación RMI: " + e.getMessage());
                System.out.println("  Verifique que el servidor está ejecutándose.\n");
            } catch (Exception e) {
                System.err.println("Error inesperado: " + e.getMessage() + "\n");
            }
        }
        
        scanner.close();
    }
    
    private void mostrarMenu() {
        System.out.println("========================================");
        System.out.println("   SISTEMA DE TARJETAS DE CREDITO RMI   ");
        System.out.println("========================================");
        System.out.println("[1] Crear nueva cuenta");
        System.out.println("[2] Realizar cargo (compra)");
        System.out.println("[3] Realizar pago");
        System.out.println("[4] Consultar saldo");
        System.out.println("[5] Ver historial de transacciones");
        System.out.println("[6] Mostrar información de cuenta");
        System.out.println("[0] Salir");
        System.out.println("========================================\n");
    }
    
    private void crearCuenta() throws RemoteException {
        System.out.println("\n--- CREAR NUEVA CUENTA ---");
        
        System.out.print("Número de cuenta: ");
        String numero = scanner.nextLine().trim();
        
        System.out.print("Nombre del titular: ");
        String nombre = scanner.nextLine().trim();
        
        System.out.print("Límite de crédito ($): ");
        double limite = obtenerDouble();
        
        if (servicio.crearCuenta(numero, nombre, limite)) {
            System.out.println("Cuenta creada exitosamente\n");
        }
    }
    
    private void realizarCargo() throws RemoteException {
        System.out.println("\n--- REALIZAR CARGO (COMPRA) ---");
        
        System.out.print("Número de cuenta: ");
        String numero = scanner.nextLine().trim();
        
        if (!servicio.existeCuenta(numero)) {
            throw new IllegalArgumentException("La cuenta no existe");
        }
        
        System.out.print("Monto ($): ");
        double monto = obtenerDouble();
        
        System.out.print("Descripción (Ej: Compra en tienda): ");
        String descripcion = scanner.nextLine().trim();
        if (descripcion.isEmpty()) {
            descripcion = "Cargo sin descripción";
        }
        
        if (servicio.realizarCargo(numero, monto, descripcion)) {
            System.out.println("Cargo realizado exitosamente");
            System.out.println("  Nuevo saldo: $" + String.format("%.2f", servicio.obtenerSaldo(numero)) + "\n");
        }
    }
    
    private void realizarPago() throws RemoteException {
        System.out.println("\n--- REALIZAR PAGO ---");
        
        System.out.print("Número de cuenta: ");
        String numero = scanner.nextLine().trim();
        
        if (!servicio.existeCuenta(numero)) {
            throw new IllegalArgumentException("La cuenta no existe");
        }
        
        double saldoActual = servicio.obtenerSaldo(numero);
        System.out.println("  Saldo actual: $" + String.format("%.2f", saldoActual));
        
        if (saldoActual >= 0) {
            System.out.println("No hay deuda pendiente");
            return;
        }
        
        System.out.println("  Deuda a pagar: $" + String.format("%.2f", Math.abs(saldoActual)));
        System.out.print("Monto a pagar ($): ");
        double monto = obtenerDouble();
        
        System.out.print("Descripción (Ej: Pago en línea): ");
        String descripcion = scanner.nextLine().trim();
        if (descripcion.isEmpty()) {
            descripcion = "Pago sin descripción";
        }
        
        if (servicio.realizarPago(numero, monto, descripcion)) {
            System.out.println("Pago realizado exitosamente");
            double nuevoSaldo = servicio.obtenerSaldo(numero);
            System.out.println("  Nuevo saldo: $" + String.format("%.2f", nuevoSaldo) + "\n");
        }
    }
    
    private void consultarSaldo() throws RemoteException {
        System.out.println("\n--- CONSULTAR SALDO ---");
        
        System.out.print("Número de cuenta: ");
        String numero = scanner.nextLine().trim();
        
        double saldo = servicio.obtenerSaldo(numero);
        
        System.out.println("\n  Número de cuenta: " + numero);
        if (saldo == 0) {
            System.out.println("  Estado: Sin deuda");
        } else if (saldo < 0) {
            System.out.println("  Deuda: $" + String.format("%.2f", Math.abs(saldo)));
        } else {
            System.out.println("  A su favor: $" + String.format("%.2f", saldo));
        }
        System.out.println();
    }
    
    private void verHistorial() throws RemoteException {
        System.out.println("\n--- HISTORIAL DE TRANSACCIONES ---");
        
        System.out.print("Número de cuenta: ");
        String numero = scanner.nextLine().trim();
        
        TransactionRecord[] historial = servicio.obtenerHistorial(numero);
        
        if (historial.length == 0) {
            System.out.println("  No hay transacciones registradas\n");
            return;
        }
        
        System.out.println("\n  Total de transacciones: " + historial.length);
        System.out.println("  " + "─".repeat(80));
        
        for (TransactionRecord transaccion : historial) {
            System.out.println("  " + transaccion.toString());
        }
        
        System.out.println();
    }
    
    private void mostrarInfoCuenta() throws RemoteException {
        System.out.println("\n--- INFORMACIÓN DE CUENTA ---");
        
        System.out.print("Número de cuenta: ");
        String numero = scanner.nextLine().trim();
        
        String info = servicio.obtenerInfoCuenta(numero);
        System.out.println(info);
    }
    
    /**
     * Método auxiliar para obtener un double válido del usuario.
     */
    private double obtenerDouble() {
        while (true) {
            try {
                String input = scanner.nextLine().trim();
                double valor = Double.parseDouble(input);
                if (valor <= 0) {
                    System.out.print("Debe ser un número positivo: ");
                    continue;
                }
                return valor;
            } catch (NumberFormatException e) {
                System.out.print("Entrada inválida. Ingrese un número: ");
            }
        }
    }
    
    /**
     * Punto de entrada de la aplicación cliente.
     */
    public static void main(String[] args) {
        try {
            System.out.println("═══════════════════════════════════════════════");
            System.out.println("   CLIENTE RMI - TARJETAS DE CRÉDITO");
            System.out.println("═══════════════════════════════════════════════\n");
            
            // Crear cliente y conectar
            CreditCardClient cliente = new CreditCardClient();
            
            // Iniciar menú interactivo
            cliente.iniciar();
            
        } catch (RemoteException e) {
            System.err.println("Error de comunicación RMI");
            System.err.println("  Verifique que:");
            System.err.println("  1. El servidor está ejecutándose");
            System.err.println("  2. El RMI Registry está activo en puerto 1099");
            System.err.println("  Detalles: " + e.getMessage());
            System.exit(1);
        } catch (NotBoundException e) {
            System.err.println("El servicio 'CreditCardService' no está registrado");
            System.err.println("  Verifique que el servidor está ejecutándose");
            System.exit(1);
        }
    }
}
