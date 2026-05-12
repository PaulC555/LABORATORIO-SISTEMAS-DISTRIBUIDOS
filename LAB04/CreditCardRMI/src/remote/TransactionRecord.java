package remote;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Clase serializable que representa un registro de transacción.
 * Implementa Serializable para poder ser transmitida a través de RMI.
 */
public class TransactionRecord implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String tipo;              // "CARGO" o "PAGO"
    private double monto;
    private LocalDateTime fecha;
    private String descripcion;
    private double saldoResultante;

    /**
     * Constructor para crear un registro de transacción.
     *
     * @param tipo tipo de transacción (CARGO o PAGO)
     * @param monto cantidad de la transacción
     * @param fecha fecha y hora de la transacción
     * @param descripcion descripción de la transacción
     * @param saldoResultante saldo después de la transacción
     */
    public TransactionRecord(String tipo, double monto, LocalDateTime fecha,
                           String descripcion, double saldoResultante) {
        this.tipo = tipo;
        this.monto = monto;
        this.fecha = fecha;
        this.descripcion = descripcion;
        this.saldoResultante = saldoResultante;
    }

    // Getters
    public String getTipo() {
        return tipo;
    }

    public double getMonto() {
        return monto;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public double getSaldoResultante() {
        return saldoResultante;
    }

    @Override
    public String toString() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return String.format("[%s] %s | Monto: $%.2f | Saldo: $%.2f | %s",
                fecha.format(formatter), tipo, monto, saldoResultante, descripcion);
    }
}
