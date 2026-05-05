package Lab02;

public class Nodo {
    private String nombre;
    private long reloj;

    public Nodo(String nombre, long reloj) {
        this.nombre = nombre;
        this.reloj = reloj;
    }

    public long getReloj() {
        return reloj;
    }

    public void ajustar(long ajuste) {
        reloj += ajuste;
    }

    public String getNombre() {
        return nombre;
    }
}
