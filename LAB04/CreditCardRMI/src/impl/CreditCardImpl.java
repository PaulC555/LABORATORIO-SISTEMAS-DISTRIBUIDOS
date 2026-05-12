package impl;

import remote.CreditCardRemote;
import remote.TransactionRecord;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Implementación del servicio remoto de tarjetas de crédito.
 * 
 * Esta clase implementa la lógica de negocio para operaciones de tarjetas de crédito.
 * Extiende UnicastRemoteObject para ser accesible por RMI.
 * 
 * Patrón RMI:
 * - Extiende UnicastRemoteObject
 * - Implementa la interfaz remota CreditCardRemote
 * - Exporta automáticamente al crear una instancia
 */
public class CreditCardImpl extends UnicastRemoteObject implements CreditCardRemote {
    
    private static final long serialVersionUID = 1L;
    
    // Clase interna para representar una cuenta de tarjeta
    private static class Cuenta {
        String numeroCuenta;
        String nombreTitular;
        double limiteCredito;
        double saldo;  // Negativo = deuda al banco
        List<TransactionRecord> historial;
        
        Cuenta(String numero, String nombre, double limite) {
            this.numeroCuenta = numero;
            this.nombreTitular = nombre;
            this.limiteCredito = limite;
            this.saldo = 0;  // Saldo inicial = 0
            this.historial = new ArrayList<>();
        }
    }
    
    // Base de datos en memoria de cuentas
    private Map<String, Cuenta> cuentas;
    
    /**
     * Constructor que inicializa el servicio remoto.
     * @throws RemoteException si ocurre un error en la exportación RMI
     */
    public CreditCardImpl() throws RemoteException {
        super();
        this.cuentas = Collections.synchronizedMap(new HashMap<>());
        System.out.println("Servicio CreditCardImpl inicializado");
    }
    
    /**
     * Validaciones privadas
     */
    private void validarNumeroCuenta(String numero) {
        if (numero == null || numero.trim().isEmpty()) {
            throw new IllegalArgumentException("El número de cuenta no puede estar vacío");
        }
    }
    
    private void validarMonto(double monto) {
        if (monto <= 0) {
            throw new IllegalArgumentException("El monto debe ser mayor a 0");
        }
    }
    
    private void validarCuentaExiste(String numero) {
        if (!cuentas.containsKey(numero)) {
            throw new IllegalArgumentException("La cuenta " + numero + " no existe");
        }
    }
    
    @Override
    public boolean crearCuenta(String numeroCuenta, String nombreTitular, double limiteCredito)
            throws RemoteException {
        
        try {
            // Validaciones
            validarNumeroCuenta(numeroCuenta);
            if (nombreTitular == null || nombreTitular.trim().isEmpty()) {
                throw new IllegalArgumentException("El nombre del titular no puede estar vacío");
            }
            if (limiteCredito <= 0) {
                throw new IllegalArgumentException("El límite de crédito debe ser mayor a 0");
            }
            if (cuentas.containsKey(numeroCuenta)) {
                throw new IllegalArgumentException("La cuenta " + numeroCuenta + " ya existe");
            }
            
            // Crear cuenta
            Cuenta nueva = new Cuenta(numeroCuenta, nombreTitular.trim(), limiteCredito);
            cuentas.put(numeroCuenta, nueva);
            
            System.out.println("Cuenta creada: " + numeroCuenta + " (" + nombreTitular + ")");
            return true;
            
        } catch (IllegalArgumentException e) {
            System.err.println("Error al crear cuenta: " + e.getMessage());
            throw e;
        }
    }
    
    @Override
    public double obtenerSaldo(String numeroCuenta) throws RemoteException {
        validarNumeroCuenta(numeroCuenta);
        validarCuentaExiste(numeroCuenta);
        
        return cuentas.get(numeroCuenta).saldo;
    }
    
    @Override
    public boolean realizarCargo(String numeroCuenta, double monto, String descripcion)
            throws RemoteException {
        
        try {
            validarNumeroCuenta(numeroCuenta);
            validarMonto(monto);
            validarCuentaExiste(numeroCuenta);
            
            Cuenta cuenta = cuentas.get(numeroCuenta);
            
            // Realizar cargo (permitir sobregiros sin límite)
            double nuevoSaldo = cuenta.saldo - monto;  // Negativo = deuda
            cuenta.saldo = nuevoSaldo;
            
            // Registrar transacción
            TransactionRecord transaccion = new TransactionRecord(
                "CARGO",
                monto,
                LocalDateTime.now(),
                descripcion != null ? descripcion : "Cargo sin descripción",
                cuenta.saldo
            );
            cuenta.historial.add(transaccion);
            
            System.out.println("✓ Cargo realizado - " + numeroCuenta + ": $" + monto);
            return true;
            
        } catch (IllegalArgumentException e) {
            System.err.println("✗ Error al realizar cargo: " + e.getMessage());
            throw e;
        }
    }
    
    @Override
    public boolean realizarPago(String numeroCuenta, double monto, String descripcion)
            throws RemoteException {
        
        try {
            validarNumeroCuenta(numeroCuenta);
            validarMonto(monto);
            validarCuentaExiste(numeroCuenta);
            
            Cuenta cuenta = cuentas.get(numeroCuenta);
            
            // El pago reduce la deuda (suma al saldo)
            double nuevoSaldo = cuenta.saldo + monto;
            
            // No permitir pagar más de lo que se debe
            if (nuevoSaldo > 0) {
                throw new IllegalArgumentException(
                    String.format("Pago excesivo. Deuda actual: $%.2f",
                    Math.abs(cuenta.saldo))
                );
            }
            
            // Realizar pago
            cuenta.saldo = nuevoSaldo;
            
            // Registrar transacción
            TransactionRecord transaccion = new TransactionRecord(
                "PAGO",
                monto,
                LocalDateTime.now(),
                descripcion != null ? descripcion : "Pago sin descripción",
                cuenta.saldo
            );
            cuenta.historial.add(transaccion);
            
            System.out.println("✓ Pago realizado - " + numeroCuenta + ": $" + monto);
            return true;
            
        } catch (IllegalArgumentException e) {
            System.err.println("✗ Error al realizar pago: " + e.getMessage());
            throw e;
        }
    }
    
    @Override
    public TransactionRecord[] obtenerHistorial(String numeroCuenta) throws RemoteException {
        validarNumeroCuenta(numeroCuenta);
        validarCuentaExiste(numeroCuenta);
        
        Cuenta cuenta = cuentas.get(numeroCuenta);
        return cuenta.historial.toArray(new TransactionRecord[0]);
    }
    
    @Override
    public String obtenerInfoCuenta(String numeroCuenta) throws RemoteException {
        validarNumeroCuenta(numeroCuenta);
        validarCuentaExiste(numeroCuenta);
        
        Cuenta cuenta = cuentas.get(numeroCuenta);
        StringBuilder sb = new StringBuilder();
        
        sb.append("\n╔════════════════════════════════════════╗\n");
        sb.append("║      INFORMACIÓN DE LA CUENTA          ║\n");
        sb.append("╠════════════════════════════════════════╣\n");
        sb.append(String.format("║ Número:          %-22s ║\n", numeroCuenta));
        sb.append(String.format("║ Titular:         %-22s ║\n", cuenta.nombreTitular));
        sb.append(String.format("║ Límite Crédito:  $%-21.2f║\n", cuenta.limiteCredito));
        sb.append(String.format("║ Saldo:           $%-21.2f║\n", cuenta.saldo));
        
        // Mostrar estado
        String estado;
        if (cuenta.saldo == 0) {
            estado = "Sin deuda";
        } else if (cuenta.saldo < 0) {
            estado = "Deuda: $" + String.format("%.2f", Math.abs(cuenta.saldo));
        } else {
            estado = "$ A favor: $" + String.format("%.2f", cuenta.saldo);
        }
        sb.append(String.format("║ Estado:          %-22s ║\n", estado));
        
        sb.append(String.format("║ Transacciones:   %-22d ║\n", cuenta.historial.size()));
        sb.append("╚════════════════════════════════════════╝\n");
        
        return sb.toString();
    }
    
    @Override
    public boolean existeCuenta(String numeroCuenta) throws RemoteException {
        validarNumeroCuenta(numeroCuenta);
        return cuentas.containsKey(numeroCuenta);
    }
    
    // Método auxiliar para obtener número total de cuentas (útil para administración)
    public int getTotalCuentas() {
        return cuentas.size();
    }
}
