package LAB04;

import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;

public class ServidorMoneda {

    public static void main(String[] args) {
        try {
            LocateRegistry.createRegistry(1099);

            Moneda servicio = new Moneda();

            Naming.rebind("rmi://localhost/SERVICIO_MONEDA", servicio);

            System.out.println("Servidor de conversión de moneda listo...");
        } catch (Exception e) {
            System.out.println("Error en el servidor: " + e.getMessage());
        }
    }
}
