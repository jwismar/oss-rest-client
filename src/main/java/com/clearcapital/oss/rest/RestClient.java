package com.clearcapital.oss.rest;

import java.net.URI;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

import org.glassfish.jersey.filter.LoggingFilter;

import com.clearcapital.oss.java.AssertHelpers;
import com.clearcapital.oss.java.exceptions.AssertException;
import com.clearcapital.oss.json.JsonSerializer;
import com.clearcapital.oss.json.serializers.EmptyStringAsNullIntegerKeyDeserializer;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * RestClientDriver provides a connection to a RESTful web service host, authenticating using Basic Authentication over
 * SSL. The base URI (e.g. https://api.clearcapital.com), api-key (username), and api-password should be managed in a
 * protected application configuration file.
 * 
 * Usage: (1) instantiate a RestClientDriver, passing the URI, api-key, and api-password; (2) instantiate an
 * {Entity}ResourceClient subclass of BaseClient, passing a call to the getService() method below; and (3) exercise the
 * various methods of the resource client to persist, find, etc. objects of the associated entity.
 * 
 * @author david.prinzing
 */
public class RestClient {

    private final WebTarget webTarget;

    public void flushCache() {
        // TODO: figure out how to flush the cache
    }

    public void close() {
        // TODO figure out how to close a webTarget.
    }

    public RestClient(RestClientConfiguration restEndpointConfig) throws AssertException {
        AssertHelpers.notNull(restEndpointConfig, "restEndpointConfig");
        AssertHelpers.notNull(restEndpointConfig.getKey(), "restEndpointConfig.key");
        AssertHelpers.notNull(restEndpointConfig.getPassword(), "restEndpointConfig.password");
        AssertHelpers.notNull(restEndpointConfig.getUri(), "restEndpointConfig.uri");

        if (restEndpointConfig.getDisableCertificateValidation())
            disableCertificateValidation(); // disabled to accommodate self-signed certificates

        ObjectMapper objectMapper = new ObjectMapper();
        JsonSerializer.configureObjectMapper(objectMapper);
        objectMapper.enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT);

        @SuppressWarnings("deprecation")
        SimpleModule module = new SimpleModule("EmptyStringHandlingModule", new Version(0, 1, 0, "SNAPSHOT))"));
        module.addKeyDeserializer(Integer.class, new EmptyStringAsNullIntegerKeyDeserializer());
        objectMapper.registerModule(module);

        // executor = Executors.newFixedThreadPool(1);
        // client = new JerseyClientBuilder().using(jerseyClientConfig)
        // executor = Executors.newFixedThreadPool(1);

        // client = new CachingJerseyClientBuilder(metrics).using(restEndpointConfig)
        // .withProperty(ApacheHttpClient4Config.PROPERTY_ENABLE_BUFFERING, true)
        // .using(Executors.newSingleThreadExecutor(), objectMapper).build("rest_client_driver");
        // client.addFilter(new HTTPBasicAuthFilter(apiKey, apiPassword));

        // EDE,JW - Fixes problem where repeated requests throw an odd "No connection" exception.
        // (See, e.g.:
        // http://stackoverflow.com/questions/10558791/apache-httpclient-interim-error-nohttpresponseexception)
        // This is a Band-Aid - there may still be half-closed connections out there. Ideally it
        // would be nice to identify why that's happening.
        // We used to use jerseyClientConfig.setRetries(1), but deep inside Jersey, that does this:
        // client.setHttpRequestRetryHandler(new DefaultHttpRequestRetryHandler(configuration.getRetries(), false));
        // The "false" parameter there tells jersey not to retry when it thinks a request has been sent, but
        // then gets dropped. this.setRetries() does the same thing through a series of decorators, except
        // it passes "true" for that parameter. It's kind of annoying that the config doesn't have a way to change
        // the false above into a true.
        // setRetries(1);

        URI uri = restEndpointConfig.getUri();
        webTarget = ClientBuilder.newClient(restEndpointConfig.getJaxRsConfiguration())
                .register(new HttpBasicAuthenticator(restEndpointConfig.getKey(), restEndpointConfig.getPassword()))
                .target(uri);
        if (restEndpointConfig.getWithLoggingFilter()) {
            webTarget.register(new LoggingFilter());
        }
    }
    
    public WebTarget getWebTarget() {
        return webTarget;
    }

    private void disableCertificateValidation() {
        // Create a trust manager that does not validate certificate chains
        TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }

            @Override
            public void checkClientTrusted(final X509Certificate[] certs, final String authType) {
            }

            @Override
            public void checkServerTrusted(final X509Certificate[] certs, final String authType) {
            }
        } };

        // Ignore differences between given hostname and certificate hostname
        HostnameVerifier hv = new HostnameVerifier() {

            @Override
            public boolean verify(final String hostname, final SSLSession session) {
                return true;
            }
        };

        // Install the all-trusting trust manager
        try {
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier(hv);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
        }
    }


}
