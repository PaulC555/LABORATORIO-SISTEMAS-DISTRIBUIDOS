package remote;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Interfaz remota para operaciones de tarjeta de crédito.
 * Define los métodos que los clientes pueden invocar remotamente en el servidor.
 * 
 * Patrón RMI:
 * - Extiende Remote
 * - Todos los métodos pueden lanzar RemoteException
 * - Solo incluye métodos que serán invocados remotamente
 */
public interface CreditCardRemote extends Remote {
    
    /**
     * Crea una nueva cuenta de tarjeta de crédito.
     *
     * @param numeroCuenta número único de la tarjeta
     * @param nombreTitular nombre del titular de la tarjeta
     * @param limiteCredito límite de crédito disponible
     * @return true si la cuenta se creó exitosamente
     * @throws RemoteException si ocurre un error de comunicación remota
     * @throws IllegalArgumentException si los parámetros son inválidos
     */
    boolean crearCuenta(String numeroCuenta, String nombreTitular, double limiteCredito)
            throws RemoteException;
    
    /**
     * Obtiene el saldo actual de la tarjeta.
     *
     * @param numeroCuenta número de la tarjeta
     * @return saldo actual (negativo = deuda)
     * @throws RemoteException si ocurre un error de comunicación remota
     * @throws IllegalArgumentException si la cuenta no existe
     */
    double obtenerSaldo(String numeroCuenta) throws RemoteException;
    
    /**
     * Realiza un cargo a la tarjeta (compra).
     *
     * @param numeroCuenta número de la tarjeta
     * @param monto cantidad a cargar
     * @param descripcion descripción de la compra
     * @return true si la transacción fue exitosa
     * @throws RemoteException si ocurre un error de comunicación remota
     * @throws IllegalArgumentException si los parámetros son inválidos
     */
    boolean realizarCargo(String numeroCuenta, double monto, String descripcion)
            throws RemoteException;
    
    /**
     * Realiza un pago a la tarjeta.
     *
     * @param numeroCuenta número de la tarjeta
     * @param monto cantidad a pagar
     * @param descripcion descripción del pago
     * @return true si la transacción fue exitosa
     * @throws RemoteException si ocurre un error de comunicación remota
     * @throws IllegalArgumentException si los parámetros son inválidos
     */
    boolean realizarPago(String numeroCuenta, double monto, String descripcion)
            throws RemoteException;
    
    /**
     * Obtiene el historial de transacciones de la tarjeta.
     *
     * @param numeroCuenta número de la tarjeta
     * @return arreglo de registros de transacciones
     * @throws RemoteException si ocurre un error de comunicación remota
     * @throws IllegalArgumentException si la cuenta no existe
     */
    TransactionRecord[] obtenerHistorial(String numeroCuenta) throws RemoteException;
    
    /**
     * Obtiene información completa de la cuenta.
     *
     * @param numeroCuenta número de la tarjeta
     * @return String con información de la cuenta
     * @throws RemoteException si ocurre un error de comunicación remota
     * @throws IllegalArgumentException si la cuenta no existe
     */
    String obtenerInfoCuenta(String numeroCuenta) throws RemoteException;
    
    /**
     * Verifica si una cuenta existe.
     *
     * @param numeroCuenta número de la tarjeta
     * @return true si la cuenta existe
     * @throws RemoteException si ocurre un error de comunicación remota
     */
    boolean existeCuenta(String numeroCuenta) throws RemoteException;
}
