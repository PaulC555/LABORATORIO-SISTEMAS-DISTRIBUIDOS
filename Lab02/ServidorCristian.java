package Lab02;

import java.io.*;
import java.net.*;

public class ServidorCristian {
    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(5000);
        System.out.println("Servidor de tiempo iniciado con latencia simulada...");

        while (true) {
            Socket socket = serverSocket.accept();

            try {
                // 🔴 Simulación de latencia de red (100 ms)
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            DataOutputStream out = new DataOutputStream(socket.getOutputStream());

            long tiempoServidor = System.currentTimeMillis();
            out.writeLong(tiempoServidor);

            socket.close();
        }
    }
}
