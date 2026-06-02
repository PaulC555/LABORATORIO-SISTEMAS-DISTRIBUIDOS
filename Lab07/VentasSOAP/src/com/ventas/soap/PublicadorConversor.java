package com.ventas.soap;

import javax.xml.ws.Endpoint;

public class PublicadorConversor {

    public static void main(String[] args) {

        Endpoint.publish(
            "http://localhost:8080/conversor",
            new ConversorSOAP());

        System.out.println(
            "Servicio SOAP Conversor Activo");
    }
}