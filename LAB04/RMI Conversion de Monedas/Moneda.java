package LAB04;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class Moneda extends UnicastRemoteObject implements MonedaInterface {

    private static final double TASA_DOLAR = 3.75;
    private static final double TASA_EURO = 4.05;

    public Moneda() throws RemoteException {
        super();
    }

    @Override
    public double convertirADolares(double monto) throws RemoteException {
        return monto / TASA_DOLAR;
    }

    @Override
    public double convertirAEuros(double monto) throws RemoteException {
        return monto / TASA_EURO;
    }
}
