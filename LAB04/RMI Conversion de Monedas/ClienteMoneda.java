package LAB04;

import java.rmi.Naming;
import java.util.Scanner;

public class ClienteMoneda {

    public static void main(String[] args) {
        try {
            Scanner sc = new Scanner(System.in);

            MonedaInterface servicio =
                    (MonedaInterface) Naming.lookup("rmi://localhost/SERVICIO_MONEDA");

            System.out.print("Ingrese monto en soles: ");
            double monto = sc.nextDouble();

            System.out.println("Seleccione opción:");
            System.out.println("1. Convertir a dólares");
            System.out.println("2. Convertir a euros");

            int opcion = sc.nextInt();

            if (opcion == 1) {
                double dolares = servicio.convertirADolares(monto);
                System.out.println("Monto en dólares: $" + dolares);
            } else if (opcion == 2) {
                double euros = servicio.convertirAEuros(monto);
                System.out.println("Monto en euros: €" + euros);
            } else {
                System.out.println("Opción inválida");
            }

            sc.close();

        } catch (Exception e) {
            System.out.println("Error en el cliente: " + e.getMessage());
        }
    }
}
