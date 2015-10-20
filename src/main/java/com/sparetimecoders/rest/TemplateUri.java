package com.sparetimecoders.rest;

import com.damnhandy.uri.template.UriTemplate;

/**
 * TemplateUri evaluates a template expression and returns the result
 */
public class TemplateUri extends Uri {
    private final UriTemplate uri;

    public TemplateUri(String uri) {
        this.uri = UriTemplate.fromTemplate(uri);
    }


    public TemplateUri set(String variableName, Object value) {
        uri.set(variableName, value);
        return this;
    }

    @Override
    public String toString() {
        return uri.expand();
    }

}
