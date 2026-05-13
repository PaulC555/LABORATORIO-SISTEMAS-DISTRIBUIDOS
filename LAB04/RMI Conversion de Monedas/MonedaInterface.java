package LAB04;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface MonedaInterface extends Remote {
    double convertirADolares(double monto) throws RemoteException;
    double convertirAEuros(double monto) throws RemoteException;
}