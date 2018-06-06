package com.sparetimecoders.rest;

import org.asynchttpclient.AsyncHttpClient;
import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

public class AbstractJsonRestClientTest {
    @Test
    public void closeClosesConnection() throws IOException {
        RestClient client = new RestClient(null, "url", "username", "password");
        assertFalse(client.isClosed());
        client.close();
        assertTrue(client.isClosed());
    }

    @Test
    public void templateUriEvaluatesCorrect() {
        RestClient client = new RestClient(null, "url/", "username", "password");
        TemplateUri template = client.template("/{id}/{value}");
        template.set("id", 1);
        template.set("value", "v");
        assertThat(template.toString(), is("url/1/v"));
    }

    @Test
    public void constantUriReturnsString() {
        RestClient client = new RestClient(null, "url/", "username", "password");
        Uri template = client.constant("/{id}/{value}");
        assertThat(template.toString(), is("url/{id}/{value}"));
    }

    private static class RestClient extends AbstractJsonRestClient<ClientException, ClientResponseException> {
        public RestClient(AsyncHttpClient client, String url, String username, String password) {
            super(client, url, username, password);
        }
    }

}
