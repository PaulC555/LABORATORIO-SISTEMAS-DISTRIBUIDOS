package com.ventas.soap;

public class PruebaVentas {

    public static void main(String[] args) {

        VentasSOAP ventas = new VentasSOAP();

        System.out.println(ventas.calcularTotal(50,4));

        System.out.println(ventas.aplicarDescuento(600));

        System.out.println(
            ventas.registrarVenta(
                "Laptop",
                2,
                2500));
    }
}