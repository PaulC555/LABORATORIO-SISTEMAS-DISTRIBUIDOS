package com.ventas.soap;

import javax.jws.WebMethod;
import javax.jws.WebService;

@WebService
public class ConversorSOAP {

    @WebMethod
    public double cToF(double c) {
        return (c * 9 / 5) + 32;
    }

    @WebMethod
    public double fToC(double f) {
        return (f - 32) * 5 / 9;
    }
}