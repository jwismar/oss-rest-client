package com.clearcapital.oss.rest;

import java.net.URI;

import javax.ws.rs.core.Configuration;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RestClientConfiguration {

    private URI uri;
    private String key;
    private String password;
    private boolean disableCertificateValidation;
    private Boolean withLoggingFilter;
    private Configuration jaxRsConfiguration;

    public RestClientConfiguration() {

    }

    public static Builder builder() {
        return new Builder();
    }

    @JsonProperty
    public URI getUri() {
        return uri;
    }

    @JsonProperty
    public String getKey() {
        return key;
    }

    @JsonProperty
    public String getPassword() {
        return password;
    }

    @JsonProperty
    public Boolean getWithLoggingFilter() {
        return withLoggingFilter;
    }

    public boolean getDisableCertificateValidation() {
        return disableCertificateValidation;
    }

    public static class Builder {

        RestClientConfiguration result;

        Builder() {
            result = new RestClientConfiguration();
        }

        public Builder setDisableCertificateValidation(boolean value) {
            result.disableCertificateValidation = value;
            return this;
        }

        public Builder setJaxRsConfiguration(Configuration value) {
            result.jaxRsConfiguration = value;
            return this;
        }

        public Builder setKey(String key) {
            result.key = key;
            return this;
        }

        public Builder setPassword(String password) {
            result.password = password;
            return this;
        }

        public Builder setUri(URI uri) {
            result.uri = uri;
            return this;
        }

        public Builder setWithLoggingFilter(Boolean value) {
            result.withLoggingFilter = value;
            return this;
        }

        public RestClientConfiguration build() {
            return result;
        }
    }

    public Configuration getJaxRsConfiguration() {
        return jaxRsConfiguration;
    }

}
