package com.clearcapital.oss.rest;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response.Status;

import org.apache.http.entity.ContentType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.clearcapital.oss.json.JsonSerializer;
import com.github.tomakehurst.wiremock.client.ValueMatchingStrategy;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.common.collect.ImmutableList;

public class BaseResourceClientTest {

    private static final String QUERY_PARAM_NAME = "jennysnumber";
    private static final String JENNYS_NUMBER = "867-5309";
    private static final String APPLICATION_JSON = ContentType.APPLICATION_JSON.getMimeType();
    private static final String CONTENT_TYPE = "Content-Type";
    private static final String COOKIE = "Cookie";
    private static final String BASE_URI = "http://localhost:5309";
    private static final String V1_ENTRIES = "/v1/entries";
    private BaseResourceClient<DemoRestableObject> client;

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(5309);

    @Before
    public void beforeTest() {
        WebTarget webTarget = ClientBuilder.newClient().target(BASE_URI);
        client = new BaseResourceClient<>(webTarget, DemoRestableObject.class, V1_ENTRIES);
    }

    private ValueMatchingStrategy buildSessionCookieMatcher() {
        ValueMatchingStrategy cookieMatcher = new ValueMatchingStrategy();
        cookieMatcher.setContains("X-SessionId=1"); // jersey may add some cookie version info, so we cannot use
                                                    // setEqualTo
        return cookieMatcher;
    }

    // TODO: initialize result properly
    private MultivaluedMap<String, String> buildQueryParams() {
        MultivaluedMap<String, String> result = new MultivaluedHashMap<>();
        result.putSingle(QUERY_PARAM_NAME, JENNYS_NUMBER);
        return result;
    }

    // TODO: initialize result properly
    private MultivaluedMap<String, String> buildBrokenQueryParams() {
        MultivaluedMap<String, String> result = new MultivaluedHashMap<>();
        result.putSingle(QUERY_PARAM_NAME + "_broken", JENNYS_NUMBER);
        return result;
    }

    @Test
    public void testAvailable() throws Exception {
        Boolean entity = true;
        stubFor(get(urlPathEqualTo(V1_ENTRIES + "/1/available"))
                .willReturn(aResponse().withStatus(Status.OK.getStatusCode()).withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(JsonSerializer.getInstance().getStringRepresentation(entity))));

        assertTrue(client.available(1L));
    }

    @Test
    public void testAvailableSession() throws Exception {
        Boolean entity = true;
        stubFor(get(urlPathEqualTo(V1_ENTRIES + "/1/available")).withHeader(COOKIE, buildSessionCookieMatcher())
                .willReturn(aResponse().withStatus(Status.OK.getStatusCode()).withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(JsonSerializer.getInstance().getStringRepresentation(entity))));

        assertTrue(client.available(1L, 1L));

        // Make sure that we've mocked things correctly — that is, that our mock really is checking the cookie.
        try {
            client.available(1L);
            fail("Should've thrown");
        } catch (NotFoundException e) {
            // (yey)
        }
    }

    @Test
    public void testCreate() throws Exception {
        DemoRestableObject entity = DemoRestableObject.builder().setEntry("foo").build();
        stubFor(post(urlPathEqualTo(V1_ENTRIES)).willReturn(
                aResponse().withStatus(Status.CREATED.getStatusCode()).withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(JsonSerializer.getInstance().getStringRepresentation(entity))));

        assertEquals(entity, client.create(entity));
    }

    @Test
    public void testCreateWithSession() throws Exception {
        DemoRestableObject entity = DemoRestableObject.builder().setEntry("foo").build();
        stubFor(post(urlPathEqualTo(V1_ENTRIES)).withHeader(COOKIE, buildSessionCookieMatcher())
                .willReturn(aResponse().withStatus(Status.CREATED.getStatusCode())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(JsonSerializer.getInstance().getStringRepresentation(entity))));

        assertEquals(entity, client.create(entity, 1L));
        // Make sure that we've mocked things correctly — that is, that our mock really is checking the cookie.
        try {
            client.create(entity);
            fail("Should've thrown");
        } catch (NotFoundException e) {
            // (yey)
        }
    }

    @Test
    public void testCreateQueryParamsSession() throws Exception {
        DemoRestableObject entity = DemoRestableObject.builder().setEntry("foo").build();
        stubFor(post(urlPathEqualTo(V1_ENTRIES))
                .withHeader(COOKIE, buildSessionCookieMatcher())
                .withQueryParam(QUERY_PARAM_NAME, containing(JENNYS_NUMBER))
                .willReturn(aResponse().withStatus(Status.CREATED.getStatusCode())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(JsonSerializer.getInstance().getStringRepresentation(entity))));

        assertEquals(entity, client.create(entity, buildQueryParams(), 1L));

        // Make sure that we've mocked things correctly — that is, that our mock really is checking the cookie.
        try {
            client.create(entity, buildQueryParams(), 2L);
            fail("Should've thrown");
        } catch (NotFoundException e) {
            // (yey)
        }

        // Make sure that we've mocked things correctly — that is, that our mock really is checking the queryParams.
        try {
            client.create(entity, buildBrokenQueryParams(),1L);
            fail("Should've thrown");
        } catch (NotFoundException e) {
            // (yey)
        }
    }

    @Test
    public void testCreateEmptyListWithSession() throws Exception {
        stubFor(post(urlPathEqualTo(V1_ENTRIES)).willReturn(
                aResponse().withStatus(Status.CREATED.getStatusCode()).withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(JsonSerializer.getInstance().getStringRepresentation(ImmutableList.<DemoRestableObject> of()))));

        assertEquals(ImmutableList.<DemoRestableObject> of(), client.createList(1L,DemoRestableObject.listGenericType));
    }

    
    @Test
    public void testReadById() throws Exception {
        DemoRestableObject entity = DemoRestableObject.builder().setEntry("foo").build();
        stubFor(get(urlPathEqualTo(V1_ENTRIES + "/1"))
                .willReturn(aResponse().withStatus(Status.OK.getStatusCode()).withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(JsonSerializer.getInstance().getStringRepresentation(entity))));

        assertEquals(entity, client.read(1L));
    }

    @Test
    public void testReadByIdSession() throws Exception {
        DemoRestableObject entity = DemoRestableObject.builder().setEntry("foo").build();
        stubFor(get(urlPathEqualTo(V1_ENTRIES + "/1")).withHeader(COOKIE, buildSessionCookieMatcher())
                .willReturn(aResponse().withStatus(Status.OK.getStatusCode()).withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(JsonSerializer.getInstance().getStringRepresentation(entity))));

        assertEquals(entity, client.read(1L, 1L));

        // Make sure that we've mocked things correctly — that is, that our mock really is checking the cookie.
        try {
            client.read(1L, 2L);
            fail("Should've thrown");
        } catch (NotFoundException e) {
            // (yey)
        }
    }

    @Test
    public void testReadList() throws Exception {
        ImmutableList<DemoRestableObject> entity = ImmutableList.of(DemoRestableObject.builder().setEntry("a").build(),
                DemoRestableObject.builder().setEntry("b").build());
        stubFor(get(urlPathEqualTo(V1_ENTRIES))
                .willReturn(aResponse().withStatus(Status.OK.getStatusCode()).withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(JsonSerializer.getInstance().getStringRepresentation(entity))));

        assertEquals(entity, client.readList(DemoRestableObject.listGenericType));
    }

    @Test
    public void testReadListSession() throws Exception {
        ImmutableList<DemoRestableObject> entity = ImmutableList.of(DemoRestableObject.builder().setEntry("a").build(),
                DemoRestableObject.builder().setEntry("b").build());
        stubFor(get(urlPathEqualTo(V1_ENTRIES)).withHeader(COOKIE, buildSessionCookieMatcher())
                .willReturn(aResponse().withStatus(Status.OK.getStatusCode()).withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(JsonSerializer.getInstance().getStringRepresentation(entity))));

        assertEquals(entity, client.readList(1L, DemoRestableObject.listGenericType));

        // Make sure that we've mocked things correctly — that is, that our mock really is checking the cookie.
        try {
            client.readList(2L, DemoRestableObject.listGenericType);
            fail("Should've thrown");
        } catch (NotFoundException e) {
            // (yey)
        }
    }
}
