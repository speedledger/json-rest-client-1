package com.sparetimecoders.rest;

public class ClientException extends RuntimeException {

    private static final long serialVersionUID = 8537457195892038863L;

    public ClientException(String message, Throwable t) {
        super(message, t);
    }
}
