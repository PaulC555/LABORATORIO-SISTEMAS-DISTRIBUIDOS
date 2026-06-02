package com.ventas.soap;

import javax.xml.ws.Endpoint;

public class Publicador {

    public static void main(String[] args) {

        Endpoint.publish(
                "http://localhost:8080/ventas",
                new VentasSOAP());

        System.out.println(
                "Servicio SOAP de Ventas Activo");
    }
}