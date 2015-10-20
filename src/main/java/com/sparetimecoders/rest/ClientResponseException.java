package com.sparetimecoders.rest;

import com.ning.http.client.Response;

import java.io.IOException;
import java.text.MessageFormat;

public class ClientResponseException extends ClientException {

    private static final long serialVersionUID = -4703464152309784389L;

    private final int statusCode;
    private final String statusText;
    private final String body;

    public ClientResponseException(Response resp) throws IOException {
        this(resp.getStatusCode(), resp.getStatusText(), resp.getResponseBody());
    }

    private ClientResponseException(int statusCode, String statusText, String body) {
        super(MessageFormat.format("HTTP/{0}: {1}", statusCode, statusText), null);
        this.statusCode = statusCode;
        this.statusText = statusText;
        this.body = body;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getStatusText() {
        return statusText;
    }

    public String getBody() {
        return body;
    }
}
