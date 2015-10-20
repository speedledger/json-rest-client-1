package com.sparetimecoders.rest;

/**
 * Simple placeholder for am Uri
 */
public class ConstantUri extends Uri {
    private final String url;

    ConstantUri(String url) {
        this.url = url;
    }

    @Override
    public String toString() {
        return url;
    }
}
