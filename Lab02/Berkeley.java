package Lab02;

import java.util.*;

public class Berkeley {
    public static void main(String[] args) {

        List<Nodo> nodos = new ArrayList<>();

        nodos.add(new Nodo("Nodo1", 1000));
        nodos.add(new Nodo("Nodo2", 1200));
        nodos.add(new Nodo("Nodo3", 800));

        long suma = 0;

        for (Nodo n : nodos) {
            suma += n.getReloj();
        }

        long promedio = suma / nodos.size();

        System.out.println("Promedio: " + promedio);

        for (Nodo n : nodos) {
            long ajuste = promedio - n.getReloj();
            n.ajustar(ajuste);

            System.out.println(n.getNombre() + " ajustado a: " + n.getReloj());
        }
    }
}
