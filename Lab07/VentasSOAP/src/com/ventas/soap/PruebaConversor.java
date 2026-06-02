package com.ventas.soap;

public class PruebaConversor {

    public static void main(String[] args) {

        ConversorSOAP calc = new ConversorSOAP();

        System.out.println(
            calc.cToF(30));

        System.out.println(
            calc.fToC(86));
    }
}