package com.ventas.soap;

import javax.jws.WebMethod;
import javax.jws.WebService;

@WebService
public class VentasSOAP {

    @WebMethod
    public double calcularTotal(double precio, int cantidad) {
        return precio * cantidad;
    }

    @WebMethod
    public double aplicarDescuento(double total) {

        if(total >= 500) {
            return total * 0.90;
        }

        return total;
    }

    @WebMethod
    public String registrarVenta(String producto,
                                 int cantidad,
                                 double precio) {

        double total = precio * cantidad;

        return "Venta registrada: "
                + producto
                + " Cantidad: "
                + cantidad
                + " Total: S/."
                + total;
    }
}