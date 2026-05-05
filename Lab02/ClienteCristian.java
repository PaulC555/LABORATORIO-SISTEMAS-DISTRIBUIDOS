package Lab02;

import java.io.*;
import java.net.*;

public class ClienteCristian {
    public static void main(String[] args) throws IOException {

        Socket socket = new Socket("localhost", 5000);

        // Tiempo antes de enviar solicitud
        long t0 = System.currentTimeMillis();

        DataInputStream in = new DataInputStream(socket.getInputStream());
        long tiempoServidor = in.readLong();

        // Tiempo después de recibir respuesta
        long t1 = System.currentTimeMillis();

        long RTT = t1 - t0;
        long tiempoAjustado = tiempoServidor + (RTT / 2);

        System.out.println("Hora servidor: " + tiempoServidor);
        System.out.println("RTT: " + RTT + " ms");
        System.out.println("Hora ajustada: " + tiempoAjustado);

        socket.close();
    }
}