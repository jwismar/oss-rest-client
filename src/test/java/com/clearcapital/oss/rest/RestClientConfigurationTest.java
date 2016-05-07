package com.clearcapital.oss.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.net.URI;

import org.junit.Ignore;
import org.junit.Test;
import org.yaml.snakeyaml.Yaml;

import com.clearcapital.oss.json.JsonSerializer;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

public class RestClientConfigurationTest {

    @Test
    public void testDeserialize() throws Exception{
        String json =//@formatter:off
                "{\"uri\":\"http://localhost:8080/\"," 
                + "\"key\":\"someone@somewhere.com\","
                + "\"password\":\"Passw0rd\"," 
                + "\"withLoggingFilter\":false}"; // @formatter:on

        RestClientConfiguration conf = JsonSerializer.getInstance().getObject(json, RestClientConfiguration.class);
        assertNotNull(conf);
        assertEquals(new URI("http://localhost:8080/"), conf.getUri());
        assertEquals("someone@somewhere.com", conf.getKey());
        assertEquals("Passw0rd", conf.getPassword());
        assertEquals(false, conf.getWithLoggingFilter());
    }

    @Ignore("We expect this to fail, because SnakeYaml sucks")
    @Test
    public void testDeserializeYaml() throws Exception {
        String json =//@formatter:off
                "uri: http://localhost:8080/\n" 
                + "key: someone@somewhere.com\n"
                + "password: Passw0rd\n" 
                + "withLoggingFilter: false"; // @formatter:on

        Yaml yaml = new Yaml();
        RestClientConfiguration conf = yaml.loadAs(json, RestClientConfiguration.class);

        assertNotNull(conf);
        assertEquals(new URI("http://localhost:8080/"), conf.getUri());
        assertEquals("someone@somewhere.com", conf.getKey());
        assertEquals("Passw0rd", conf.getPassword());
        assertEquals(false, conf.getWithLoggingFilter());
    }

    @Test
    public void testDeserializeYamlJackson() throws Exception {
        String json =//@formatter:off
                "uri: http://localhost:8080/\n" 
                + "key: someone@somewhere.com\n"
                + "password: Passw0rd\n" 
                + "withLoggingFilter: false"; // @formatter:on

        ObjectMapper om = new ObjectMapper(new YAMLFactory());
        RestClientConfiguration conf = om.readValue(json, RestClientConfiguration.class);

        assertNotNull(conf);
        assertEquals(new URI("http://localhost:8080/"), conf.getUri());
        assertEquals("someone@somewhere.com", conf.getKey());
        assertEquals("Passw0rd", conf.getPassword());
        assertEquals(false, conf.getWithLoggingFilter());
    }

    static public class Holder {

        @JsonProperty
        RestClientConfiguration conf;
    }

    @Test
    public void testDeserializeAsChild() throws Exception {
        String json =//@formatter:off
                "{ \"conf\":{\"uri\":\"http://localhost:8080/\"," 
                + "\"key\":\"someone@somewhere.com\","
                + "\"password\":\"Passw0rd\"," 
                + "\"withLoggingFilter\":false} }"; // @formatter:on

        Holder holder = JsonSerializer.getInstance().getObject(json, Holder.class);
        assertNotNull(holder);
        RestClientConfiguration conf = holder.conf;
        assertNotNull(conf);
        assertEquals(new URI("http://localhost:8080/"), conf.getUri());
        assertEquals("someone@somewhere.com", conf.getKey());
        assertEquals("Passw0rd", conf.getPassword());
        assertEquals(false, conf.getWithLoggingFilter());
    }
}
