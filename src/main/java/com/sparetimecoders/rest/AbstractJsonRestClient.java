package com.sparetimecoders.rest;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.asynchttpclient.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

public class AbstractJsonRestClient<CE extends ClientException, RE extends ClientResponseException> implements Closeable {
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    protected final AsyncHttpClient asyncClient;
    private final boolean closeClient;
    private boolean closed = false;
    private final Realm realm;
    private final ObjectMapper mapper;
    private final String url;
    private final Class<RE> responseExceptionType;
    private final Class<CE> clientExceptionType;

    @SuppressWarnings("unchecked")
    public AbstractJsonRestClient(AsyncHttpClient client, String url, String username, String password) {
        this.clientExceptionType = (Class<CE>) ((ParameterizedType) getClass()
                .getGenericSuperclass()).getActualTypeArguments()[0];
        this.responseExceptionType = (Class<RE>) ((ParameterizedType) getClass()
                .getGenericSuperclass()).getActualTypeArguments()[1];

        this.closeClient = client == null;
        this.asyncClient = client == null ? createDefaultAsyncHttpClient() : client;
        if (url == null) {
            throw new IllegalStateException("Must specify url");
        }
        this.url = url.endsWith("/") ? StringUtils.substringBeforeLast(url, "/") : url;
        if (username == null || password == null) {
            throw new IllegalStateException("Must specify username and password");
        }
        this.realm = new Realm.Builder(username, password)
                .setScheme(Realm.AuthScheme.BASIC)
                .setUsePreemptiveAuth(true)
                .build();
        this.mapper = createMapper();
        logger.info("Creating a client connection with url: {}", url);
    }

    protected AsyncHttpClient createDefaultAsyncHttpClient() {
        return new DefaultAsyncHttpClient();
    }

    /**
     * @return true if the asyncClient connection has been closed.
     */
    public boolean isClosed() {
        return closed || asyncClient.isClosed();
    }

    @Override
    public void close() throws IOException {
        if (closeClient && !asyncClient.isClosed()) {
            asyncClient.close();
        }
        closed = true;
    }

    protected ObjectMapper createMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING);
        mapper.enable(DeserializationFeature.READ_ENUMS_USING_TO_STRING);
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return mapper;
    }

    protected ConstantUri constant(String template) {
        return new ConstantUri(url + template);
    }

    protected TemplateUri template(String template) {
        return new TemplateUri(url + template);
    }

    private static final Pattern RESTRICTED_PATTERN = Pattern.compile("%2B", Pattern.LITERAL);

    @SafeVarargs
    protected final Request req(String method, Uri template, Pair<String, String>... headers) {
        RequestBuilder builder = new RequestBuilder(method);
        builder.setRealm(realm);
        if (headers != null) {
            for (Pair<String, String> pair : headers) {
                builder.addHeader(pair.getLeft(), pair.getRight());
            }
        }
        builder.setUrl(template.toString());
        return builder.build();
    }

    protected final Request req(String method, Uri template, String contentType, byte[] body) {
        RequestBuilder builder = new RequestBuilder(method);
        builder.setRealm(realm);
        builder.setUrl(RESTRICTED_PATTERN.matcher(template.toString()).replaceAll("+")); //replace out %2B with + due to API restriction
        builder.addHeader("Content-type", contentType);
        builder.setBody(body);
        return builder.build();
    }


    protected byte[] json(Object object) {
        try {
            return mapper.writeValueAsBytes(object);
        } catch (JsonProcessingException e) {
            throw createException(e.getMessage(), e);
        }
    }

    protected <T> ListenableFuture<T> submit(Request request, AsyncCompletionHandler<T> handler) {
        if (request.getStringData() != null) {
            logger.debug("Request {} {}\n{}", request.getMethod(), request.getUrl(), request.getStringData());
        } else if (request.getByteData() != null) {
            logger.debug("Request {} {} {} {} bytes", request.getMethod(), request.getUrl(), //
                    request.getHeaders().get("Content-type"), request.getByteData().length);
        } else {
            logger.debug("Request {} {}", request.getMethod(), request.getUrl());
        }
        return asyncClient.executeRequest(request, handler);
    }


    protected void logResponse(Response response) throws IOException {
        logger.debug("Response HTTP/{} {}\n{}", response.getStatusCode(), response.getStatusText(),
                response.getResponseBody());
        if (logger.isTraceEnabled()) {
            logger.trace("Response headers {}", response.getHeaders());
        }
    }

    protected boolean isStatus2xx(Response response) {
        return response.getStatusCode() / 100 == 2;
    }

    @SuppressWarnings("unchecked")
    protected <T> T complete(ListenableFuture<T> future) {
        try {
            return future.get();
        } catch (InterruptedException e) {
            throw createException(e.getMessage(), e);
        } catch (ExecutionException e) {
            if (responseExceptionType.isAssignableFrom(e.getCause().getClass())) {
                throw (CE) e.getCause();
            }
            throw createException(e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    protected <T> AsyncCompletionHandler<T> handle(final Class<T> clazz) {
        return new AsyncCompletionHandler<T>() {
            @Override
            public T onCompleted(Response response) throws Exception {
                logResponse(response);
                if (isStatus2xx(response)) {
                    return (T) mapper.reader(clazz).readValue(response.getResponseBodyAsStream());
                }
                if (response.getStatusCode() == 404) {
                    return null;
                }
                throw createResponseException(response);
            }
        };
    }

    protected <T> AsyncCompletionHandler<List<T>> handleList(final Class<T> clazz) {
        return new AsyncCompletionHandler<List<T>>() {
            @Override
            public List<T> onCompleted(Response response) throws Exception {
                logResponse(response);
                if (isStatus2xx(response)) {
                    List<T> values = new ArrayList<>();
                    for (JsonNode node : mapper.readTree(response.getResponseBodyAsStream())) {
                        values.add(mapper.convertValue(node, clazz));
                    }
                    return values;
                }
                throw createResponseException(response);
            }
        };
    }

    private RE createResponseException(Response response) {
        try {
            return responseExceptionType.getConstructor(Response.class).newInstance(response);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create exception", e);
        }
    }


    private CE createException(String msg, Throwable t) {
        try {
            return clientExceptionType.getConstructor(String.class, Throwable.class).newInstance(msg, t);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create exception", e);
        }
    }
}
