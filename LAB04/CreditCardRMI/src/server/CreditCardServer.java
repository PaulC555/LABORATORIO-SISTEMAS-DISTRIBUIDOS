package server;

import impl.CreditCardImpl;
import remote.CreditCardRemote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

/**
 * Servidor RMI para el servicio de tarjetas de crédito.
 * 
 * Responsabilidades:
 * 1. Crear instancia del servicio remoto
 * 2. Crear/localizar el RMI Registry
 * 3. Registrar el servicio en el Registry
 * 4. Mantener el servidor ejecutándose
 */
public class CreditCardServer {
    
    private static final String NOMBRE_SERVICIO = "CreditCardService";
    private static final int PUERTO_RMI = 1099;
    
    public static void main(String[] args) {
        CreditCardImpl servicioImpl = null;
        try {
            // 1. Crear instancia del servicio
            System.out.println("═══════════════════════════════════════════════");
            System.out.println("  SERVIDOR RMI - SISTEMA DE TARJETAS DE CREDITO");
            System.out.println("═══════════════════════════════════════════════\n");
            
            System.out.println("[1] Creando instancia del servicio...");
            servicioImpl = new CreditCardImpl();
            CreditCardRemote servicioCredito = servicioImpl;
            
            // 2. Crear o localizar el RMI Registry
            System.out.println("[2] Inicializando RMI Registry en puerto " + PUERTO_RMI + "...");
            Registry registry = null;
            try {
                // Intentar crear un nuevo registry
                registry = LocateRegistry.createRegistry(PUERTO_RMI);
                System.out.println("     Nuevo Registry creado");
            } catch (RemoteException e) {
                // Si ya existe, usar el existente
                System.out.println("    Registry existente localizado");
                registry = LocateRegistry.getRegistry(PUERTO_RMI);
            }
            
            // 3. Registrar el servicio
            System.out.println("[3] Registrando servicio como '" + NOMBRE_SERVICIO + "'...");
            registry.rebind(NOMBRE_SERVICIO, servicioCredito);
            System.out.println("    Servicio registrado exitosamente");
            
            // 4. Mostrar información
            System.out.println("\n═══════════════════════════════════════════════");
            System.out.println("SERVIDOR INICIADO CORRECTAMENTE");
            System.out.println("═══════════════════════════════════════════════");
            System.out.println("Información de conexión:");
            System.out.println("  • Nombre del servicio: " + NOMBRE_SERVICIO);
            System.out.println("  • Puerto RMI: " + PUERTO_RMI);
            System.out.println("  • URL: rmi://localhost:" + PUERTO_RMI + "/" + NOMBRE_SERVICIO);
            System.out.println("\nEsperando conexiones remotas...");
            System.out.println("(Presione Ctrl+C para detener el servidor)\n");
        
        // Agregar shutdown hook para limpiar recursos y liberar puerto
        final CreditCardImpl impl = servicioImpl;
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                System.out.println("\n═══════════════════════════════════════════");
                System.out.println("  CERRANDO SERVIDOR...");
                System.out.println("═══════════════════════════════════════════");
                UnicastRemoteObject.unexportObject(impl, true);
                System.out.println("[OK] Servicio deregistrado");
                System.out.println("[OK] Puerto 1098 liberado");
                System.out.println("[OK] Recursos limpios");
                System.out.println("═══════════════════════════════════════════");
            } catch (Exception e) {
                System.err.println("Error al cerrar: " + e.getMessage());
            }
        }));
            // El servidor continuará ejecutándose mientras no haya una interrupción
            Thread.currentThread().join();
            
        } catch (RemoteException e) {
            System.err.println("✗ ERROR RMI: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        } catch (InterruptedException e) {
            System.out.println("\nServidor detenido por el usuario");
            System.exit(0);
        }
    }
}
