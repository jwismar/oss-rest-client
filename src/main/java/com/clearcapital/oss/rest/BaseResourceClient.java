package com.clearcapital.oss.rest;

import java.io.InputStream;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

/**
 * A REST client for a single resource where the server follows these conventions:
 * 
 * <li>Session id's are Long's, and if required, are passed in a cookie named X-SessionId. Obtaining a session id is
 * beyond the scope of this class.</li>
 * 
 * <li>{@code GET _baseUri_ } will return a list of {@code <T>} objects</li>
 * 
 * <li>{@code POST _baseUri_ <T> } will create a new entity, possibly filling in additional fields, and return that
 * entity.</li>
 * 
 * <li>{@code POST _baseUri_ [] } will create a new, empty list of entities, and return that list.</li>
 *
 * <li>{@code POST _baseUri_ [<T>,<T>,<T>] } will create a new list of entities, possibly filling in additional fields
 * on each entry, and return that list.</li>
 * 
 * <li>{@code PUT _baseUri_ Collection<T> } will replace the list of {@code <T>} objects at {@code _baseUri_}</li>
 * 
 * <li>{@code GET _baseUri_/_id_ } will return a single {@code <T>} object, if there is one with the specified
 * {@code _id_}. If there are multiple versions, it will return the latest one.</li>
 * 
 * <li>{@code GET _baseUri_/_id_/available } will return true if an entity with the specified {@code _id_} exists
 * 
 * <li>{@code GET _baseUri_/_key_ } will return a single {@code <T>} object, if there is one with the specified
 * {@code _key_}. If there are multiple versions, it will return the latest one.</li>
 * 
 */
public class BaseResourceClient<T> {

    private WebTarget service;
    private Class<T> clazz;
    private String uri;

    public BaseResourceClient(final WebTarget target, final Class<T> clazz, final String uri) {
        this.service = target;
        this.clazz = clazz;
        this.uri = uri;
    }

    public BaseResourceClient(final WebTarget target, final GenericType<T> gt, final String uri) {
        this.service = target;

        // This cast is a bit lame. Jersey 2 vs Jersey 1 fail.
        @SuppressWarnings("unchecked")
        Class<T> rawType = (Class<T>) gt.getRawType();

        this.clazz = rawType;
        this.uri = uri;
    }

    /**
     * Check the service to see if the specified {@code id} is available.
     * 
     * {@code GET {uri}/id/available}
     */
    public Boolean available(final Long id) {
        return service.path(uri).path(id.toString()).path("available").request().get(Boolean.class);
    }

    /**
     * Check the service to see if the specified {@code id} is available, protected by a sessionId.
     * 
     * {@code GET {uri}/id/available}
     */
    public Boolean available(final Long id, final Long sessionId) {
        return service.path(uri).path(id.toString()).path("available").request()
                .cookie("X-SessionId", sessionId.toString()).get(Boolean.class);
    }

    /**
     * POST the given entity to the base URI for this resource.
     * 
     * @return the entity, as returned by the service.
     */
    public T create(final T entity) {
        return service.path(uri).request(MediaType.APPLICATION_JSON).post(Entity.json(entity), clazz);
    }

    /**
     * POST the given entity to the base URI for this resource, passing a session id.
     * 
     * @return the entity, as returned by the service.
     */
    public T create(final T entity, final Long sessionId) {
        return service.path(uri).request(MediaType.APPLICATION_JSON).cookie("X-SessionId", sessionId.toString())
                .post(Entity.json(entity), clazz);
    }

    /**
     * POST the given entity to the base URI for this resource, passing queryParams and a session id.
     * 
     * @return the entity, as returned by the service.
     */
    public T create(final T entity, final MultivaluedMap<String, String> queryParams, final Long sessionId) {
        return applyQueryParams(service.path(uri), queryParams).request(MediaType.APPLICATION_JSON)
                .cookie("X-SessionId", sessionId.toString()).post(Entity.json(entity), clazz);
    }

    /**
     * Create with a session and id.
     * 
     * <li><b>Deprecated</b> - this is not really the right way to do things, because if you know the id/key, you really
     * ought to be using a PUT.
     */
    @Deprecated
    public T create(final Long id, final T entity, final Long sessionId) {
        return service.path(uri).path(id.toString()).request(MediaType.APPLICATION_JSON)
                .cookie("X-SessionId", sessionId.toString()).post(Entity.json(entity), clazz);
    }

    /**
     * Create with a session and key.
     * 
     * <li><b>Deprecated</b> - this is not really the right way to do things, because if you know the id/key, you really
     * ought to be using a PUT.
     */
    @Deprecated
    public T create(final String key, final T entity, final Long sessionId) {
        return service.path(uri).path(key).request(MediaType.APPLICATION_JSON)
                .cookie("X-SessionId", sessionId.toString()).post(Entity.json(entity), clazz);
    }

    /**
     * create empty list with session
     */
    public <U> U createList(final Long sessionId, final GenericType<U> superType) {
        return getSessionRequest(sessionId, MediaType.APPLICATION_JSON)
                .post(Entity.json(null), superType);
    }

    /**
     * create given list and session id
     */
    public <U> U createList(final Collection<U> requestEntity, final Long sessionId, final GenericType<U> superType) {
        return getSessionRequest(sessionId, MediaType.APPLICATION_JSON).post(Entity.json(requestEntity), superType);
    }

    public Builder getSessionRequest(Long sessionId, String... acceptedResponseTypes) {
        return service.path(uri).request(acceptedResponseTypes).cookie("X-SessionId", sessionId.toString());
    }

    // TODO: support multi-part
    // // Create - multi-part form data
    // public T createMultiPart(final FormDataMultiPart formData, final Long sessionId) {
    // return service.path(uri).type(Boundary.addBoundary(MediaType.MULTIPART_FORM_DATA_TYPE))
    // .cookie("X-SessionId" , sessionId.toString()).post(clazz, formData);
    // }

    /**
     * Create with session, expecting no content
     */
    public void createNoResponse(final T entity, final Long sessionId) {
        service.path(uri).request(MediaType.APPLICATION_JSON).cookie("X-SessionId", sessionId.toString())
                .post(Entity.json(entity));
    }

    /**
     * Delete by Id
     */
    public Response delete(final Long id) {
        return service.path(uri).path(id.toString()).request().delete();
    }

    /**
     * Delete by Id, with session
     */
    public Response delete(final Long id, final Long sessionId) {
        return service.path(uri).path(id.toString()).request().cookie("X-SessionId", sessionId.toString()).delete();
    }

    /**
     * Delete a specific version (id/updateId), given a sessionId
     */
    public Response delete(final Long id, final Long updateId, final Long sessionId) {
        return service.path(uri).path(id.toString()).path("versions").path(updateId.toString()).request()
                .cookie("X-SessionId", sessionId.toString()).delete();
    }

    /**
     * Delete given id and queryParams
     */
    public Response delete(final Long id, final MultivaluedMap<String, String> queryParams) {
        return applyQueryParams(service.path(uri).path(id.toString()), queryParams).request().delete();
    }

    /**
     * Delete by key, with session id.
     */
    public Response delete(final String key, final Long sessionId) {
        return service.path(uri).path(key.toString()).request().cookie("X-SessionId", sessionId.toString()).delete();
    }

    /**
     * Delete all, given session id.
     */
    public Response deleteAll(final Long sessionId) {
        return service.path(uri).request().cookie("X-SessionId", sessionId.toString()).delete();
    }

    /**
     * Obtain a BaseResourceClient<> for the specified {@code id, updateId} and {@code relativePath}.
     * 
     * <p>
     * For example, if {@code this.uri} is something like {@code /v1/entities}, then the result's uri will be
     * {@code /v1/entities/_id_/versions/_updateId_/_relativePath_} and the result will work with items of type
     * {@code childResourceClass}.
     * </p>
     */
    public <U> BaseResourceClient<U> getChildClient(final Class<U> childResourceClass, final Long id,
            final Long updateId, final String relativePath) {
        UriBuilder builder = UriBuilder.fromUri(uri);
        builder.path("" + id);
        builder.path("versions");
        builder.path("" + updateId);
        builder.path(relativePath);
        URI subUri = builder.build();
        return new BaseResourceClient<U>(service, childResourceClass, subUri.toString());
    }

    /**
     * Obtain a BaseResourceClient<> for the specified {@code id} and {@code relativePath}.
     * 
     * <p>
     * For example, if {@code this.uri} is something like {@code /v1/entities}, then the result's uri will be
     * {@code /v1/entities/_id_/_relativePath_} and the result will work with items of type {@code childResourceClass}.
     * </p>
     */
    public <U> BaseResourceClient<U> getChildClient(final Class<U> childResourceClass, final Long id,
            final String relativePath) {
        UriBuilder builder = UriBuilder.fromUri(uri);
        builder.path("" + id);
        builder.path(relativePath);
        URI subUri = builder.build();
        return new BaseResourceClient<U>(service, childResourceClass, subUri.toString());
    }

    /**
     * Obtain a BaseResourceClient<> for the specified {@code relativePath}.
     * 
     * <p>
     * For example, if {@code this.uri} is something like {@code /v1/entities}, then the result's uri will be
     * {@code /v1/entities/_relativePath_} and the result will work with items of type {@code childResourceClass}.
     * </p>
     */
    public <U> BaseResourceClient<U> getChildClient(final Class<U> childResourceClass, final String relativePath) {
        UriBuilder builder = UriBuilder.fromUri(uri);
        builder.path(relativePath);
        URI subUri = builder.build();
        return new BaseResourceClient<U>(service, childResourceClass, subUri.toString());
    }

    /**
     * Obtain a BaseResourceClient<> for the specified {@code key} and {@code relativePath}.
     * 
     * <p>
     * For example, if {@code this.uri} is something like {@code /v1/entities}, then the result's uri will be
     * {@code /v1/entities/_key_/_relativePath_} and the result will work with items of type {@code childResourceClass}.
     * </p>
     */
    public <U> BaseResourceClient<U> getChildClient(final Class<U> childResourceClass, final String key,
            final String relativePath) {
        UriBuilder builder = UriBuilder.fromUri(uri);
        builder.path(key);
        builder.path(relativePath);
        URI subUri = builder.build();
        return new BaseResourceClient<U>(service, childResourceClass, subUri.toString());
    }

    /**
     * Obtain a BaseResourceClient<> for the specified {@code id} and {@code relativePath}.
     * 
     * <p>
     * For example, if {@code this.uri} is something like {@code /v1/entities}, then the result's uri will be
     * {@code /v1/entities/_id_/_relativePath_} and the result will work with items of type {@code genericType}.
     * </p>
     */
    public <U> BaseResourceClient<U> getChildClient(final GenericType<U> genericType, final Long id,
            final String relativePath) {
        UriBuilder builder = UriBuilder.fromUri(uri);
        builder.path("" + id);
        builder.path(relativePath);
        URI subUri = builder.build();
        return new BaseResourceClient<U>(service, genericType, subUri.toString());
    }

    /**
     * Obtain a BaseResourceClient<> for the specified {@code key} and {@code relativePath}.
     * 
     * <p>
     * For example, if {@code this.uri} is something like {@code /v1/entities}, then the result's uri will be
     * {@code /v1/entities/_key_/_relativePath_} and the result will work with items of type {@code genericType}.
     * </p>
     */
    public <U> BaseResourceClient<U> getChildClient(final GenericType<U> genericType, final String key,
            final String relativePath) {
        UriBuilder builder = UriBuilder.fromUri(uri);
        builder.path(key);
        builder.path(relativePath);
        URI subUri = builder.build();
        return new BaseResourceClient<U>(service, genericType, subUri.toString());
    }

    /**
     * It looks like somebody put this here rather than using getChildClient with the appropriate genericType.
     * 
     * @param sessionId
     * @return
     */
    @Deprecated
    public Map<Long, Long> getCounts(final Long sessionId) {
        GenericType<Map<Long, Long>> gt = new GenericType<Map<Long, Long>>() {
        };
        return service.path(uri).request(MediaType.APPLICATION_JSON).cookie("X-SessionId", sessionId.toString())
                .get(gt);
    }

    public WebTarget getService() {
        return service;
    }

    public String getUri() {
        return uri;
    }

    /**
     * Overwrite by {@code key}, with {@code sessionId}
     * 
     * <pre>
     * {@code
     * PUT _uri_/_key
     * Cookie: X-SessionId=_sessionId_
     * 
     * _entity_
     * }
     * </pre>
     */
    public T overwrite(final String key, final T entity, final Long sessionId) {
        return service.path(uri).path(key).request(MediaType.APPLICATION_JSON)
                .cookie("X-SessionId", sessionId.toString()).put(Entity.json(entity), clazz);
    }

    /**
     * Overwrite by {@code key}, using arbitrary {@code mediaType}, with {@code sessionId}
     * 
     * <pre>
     * {@code
     * PUT _uri_/_key_
     * Cookie: X-SessionId=_sessionId_
     * Content-Type: _mediaType_
     * 
     * _entity_
     * }
     * </pre>
     */
    public T overwrite(final String key, final T entity, final MediaType mediaType, final Long sessionId) {
        return service.path(uri).path(key).request(MediaType.APPLICATION_JSON)
                .cookie("X-SessionId", sessionId.toString()).put(Entity.entity(entity, mediaType), clazz);
    }

    /**
     * Overwrite entity, given {@code sessionId}
     * 
     * <pre>
     * {@code
     * PUT _uri_
     * Cookie: X-SessionId=_sessionId_
     * 
     * _entity_
     * }
     * </pre>
     */
    public T overwrite(final T entity, final Long sessionId) {
        return service.path(uri).request(MediaType.APPLICATION_JSON).cookie("X-SessionId", sessionId.toString())
                .put(Entity.json(entity), clazz);
    }

    /**
     * Read entity
     * 
     * <pre>
     * {@code
     * GET _uri_
     * }
     * </pre>
     */
    public T read() {
        return service.path(uri).request(MediaType.APPLICATION_JSON).get(clazz);
    }

    /**
     * Get from {@code this.uri}, expecting the specified {@code genericType}, with given {@code sessionId}
     * 
     * <pre>
     * {@code
     * GET _uri_
     * Cookie: X-SessionId=_sessionId_
     * }
     * </pre>
     */
    public <U> U read(final GenericType<U> genericType, final Long sessionId) {
        return service.path(uri).request(MediaType.APPLICATION_JSON).cookie("X-SessionId", sessionId.toString())
                .get(genericType);
    }

    /**
     * Get from {@code this.uri?queryParams}, expecting the specified {@code genericType}, with given {@code sessionId}
     * 
     * <pre>
     * {@code
     * GET _uri_?_queryParams_
     * Cookie: X-SessionId=_sessionId_
     * }
     * </pre>
     */
    public <U> U read(final GenericType<U> genericType, final MultivaluedMap<String, String> queryParams,
            final Long sessionId) {
        return applyQueryParams(service.path(uri), queryParams).request(MediaType.APPLICATION_JSON)
                .cookie("X-SessionId", sessionId.toString()).get(genericType);
    }

    /**
     * Get from {@code this.uri?queryParams}, expecting the specified {@code genericType}, with given {@code sessionId}
     * 
     * <pre>
     * {@code
     * GET _uri_?_queryParams_
     * Cookie: X-SessionId=_sessionId_
     * }
     * </pre>
     */
    public T read(final Long id) {
        return service.path(uri).path(id.toString()).request(MediaType.APPLICATION_JSON).get(clazz);
    }

    /**
     * Get with given {@code id} and {@code sessionId}
     * 
     * <pre>
     * {@code
     * GET _uri_/_id_
     * Cookie: X-SessionId=_sessionId_
     * }
     * </pre>
     */
    public T read(final Long id, final Long sessionId) {
        return service.path(uri).path(id.toString()).request(MediaType.APPLICATION_JSON)
                .cookie("X-SessionId", sessionId.toString()).get(clazz);
    }

    /**
     * Get with given {@code id} and {@code sessionId}, expecting the specified {@code genericType}
     * 
     * <pre>
     * {@code
     * GET _uri_/_id_
     * Cookie: X-SessionId=_sessionId_
     * }
     * </pre>
     */
    public T read(final Long id, final Long sessionId, final GenericType<T> genericType) {
        return service.path(uri).path(id.toString()).request(MediaType.APPLICATION_JSON)
                .cookie("X-SessionId", sessionId.toString()).get(genericType);
    }

    /**
     * Get with given {@code id},{@code mediaType}, and {@code sessionId}, but return the response body as a raw string
     * rather than decoding it.
     * 
     * <li><b>Deprecated</b> This feels like it's really outside the scope of this client. Calculating the extension and
     * allowing the caller to accept raw response body is just kinda icky. The extension part more than the raw response
     * part.
     * 
     * <pre>
     * {@code
     * GET _uri_/_id_._extension_
     * Cookie: X-SessionId=_sessionId_
     * Accept: _mediaType_
     * }
     * </pre>
     */
    @Deprecated
    public String readString(final Long id, final MediaType mediaType, final Long sessionId) {
        WebTarget resource = service.path(uri);
        if (!MediaType.APPLICATION_JSON_TYPE.equals(mediaType)) {
            String extension = ".json";
            if (MediaType.APPLICATION_XML_TYPE.equals(mediaType)) {
                extension = ".xml";
            }
            resource = resource.path(id.toString() + extension);
        } else {
            resource = resource.path(id.toString());
        }
        return resource.request(mediaType).cookie("X-SessionId", sessionId.toString()).get(String.class);
    }

    /**
     * Get with given {@code id}, {@code queryParams} and {@code sessionId}
     * 
     * <pre>
     * {@code
     * GET _uri_/_id_?_queryParams_
     * Cookie: X-SessionId=_sessionId_
     * }
     * </pre>
     */
    public T read(final Long id, final MultivaluedMap<String, String> queryParams, final Long sessionId) {
        return applyQueryParams(service.path(uri).path(id.toString()), queryParams).request(MediaType.APPLICATION_JSON)
                .cookie("X-SessionId", sessionId.toString()).get(clazz);
    }

    /**
     * Get with given {@code id}, {@code queryParams} and {@code sessionId}, expecting the specified {@code genericType}
     * 
     * <pre>
     * {@code
     * GET _uri_/_id_?_queryParams_
     * Cookie: X-SessionId=_sessionId_
     * }
     * </pre>
     */
    T read(final Long id, final MultivaluedMap<String, String> queryParams, final Long sessionId,
            final GenericType<T> gt) {
        return applyQueryParams(service.path(uri).path(id.toString()), queryParams).request(MediaType.APPLICATION_JSON)
                .cookie("X-SessionId", sessionId.toString()).get(gt);
    }

    public T read(final MultivaluedMap<String, String> queryParams, final Long sessionId) {
        return applyQueryParams(service.path(uri), queryParams).request(MediaType.APPLICATION_JSON)
                .cookie("X-SessionId", sessionId.toString()).get(clazz);
    }

    // read with key
    public T read(final String key) {
        return service.path(uri).path(key).request(MediaType.APPLICATION_JSON).get(clazz);
    }

    // read with session, using a key
    public T read(final String key, final Long sessionId) {
        return service.path(uri).path(key).request(MediaType.APPLICATION_JSON)
                .cookie("X-SessionId", sessionId.toString()).get(clazz);
    }

    // read list of items
    public <U> U readList(final GenericType<U> superType) {
        return service.path(uri).request(MediaType.APPLICATION_JSON).get(superType);
    }

    // read list with session
    public <U> U readList(final Long sessionId, final GenericType<U> genericType) {
        return service.path(uri).request(MediaType.APPLICATION_JSON).cookie("X-SessionId", sessionId.toString())
                .get(genericType);
    }

    // read list with query params
    public <U> U readList(final Long sessionId, final MultivaluedMap<String, String> queryParams,
            final GenericType<U> superType) {
        return applyQueryParams(service.path(uri), queryParams).request(MediaType.APPLICATION_JSON)
                .cookie("X-SessionId", sessionId.toString()).get(superType);
    }

    // read
    public T readNoId(final Long sessionId) {
        return service.path(uri).request(MediaType.APPLICATION_JSON).cookie("X-SessionId", sessionId.toString())
                .get(clazz);
    }

    public T readNoIdAllTypes(final Long sessionId) {
        return service.path(uri).request(MediaType.WILDCARD).cookie("X-SessionId", sessionId.toString()).get(clazz);
    }

    public InputStream readStream(final Long id, final String extensionWithDot, final Long sessionId) {
        return service.path(uri).path(id.toString() + extensionWithDot).request()
                .cookie("X-SessionId", sessionId.toString()).get(InputStream.class);
    }

    public InputStream readStreamVersion(final Long id, final Long updateId, final String extensionWithDot,
            final Long sessionId) {
        return service.path(uri).path(id.toString()).path("versions").path(updateId.toString() + extensionWithDot)
                .request().cookie("X-SessionId", sessionId.toString()).get(InputStream.class);
    }

    // read version
    public T readVersion(final Long id, final Long updateId) {
        return service.path(uri).path(id.toString()).path("versions").path(updateId.toString())
                .request(MediaType.APPLICATION_JSON).get(clazz);
    }

    // read with session and version
    public T readVersion(final Long id, final Long updateId, final Long sessionId) {
        return service.path(uri).path(id.toString()).path("versions").path(updateId.toString())
                .request(MediaType.APPLICATION_JSON).cookie("X-SessionId", sessionId.toString()).get(clazz);
    }

    // read version with session, and in specified format, and return string
    public String readVersion(final Long id, final Long updateId, final MediaType mediaType, final Long sessionId) {
        WebTarget resource = service.path(uri).path(id.toString()).path("versions");
        if (!MediaType.APPLICATION_JSON_TYPE.equals(mediaType)) {
            String extension = ".json";
            if (MediaType.APPLICATION_XML_TYPE.equals(mediaType)) {
                extension = ".xml";
            }
            resource = resource.path(updateId.toString() + extension);
        } else {
            resource = resource.path(updateId.toString());
        }
        return resource.request(mediaType).cookie("X-SessionId", sessionId.toString()).get(String.class);
    }

    // read with session and version and queryParams
    public T readVersion(final Long id, final Long updateId, final MultivaluedMap<String, String> queryParams,
            final Long sessionId) {
        return applyQueryParams(service.path(uri).path(id.toString()).path("versions").path(updateId.toString()),
                queryParams).request(MediaType.APPLICATION_JSON).cookie("X-SessionId", sessionId.toString()).get(clazz);
    }

    // read with key, version, and session
    public T readVersion(final String key, final Long updateId, final Long sessionId) {
        return service.path(uri).path(key).path("versions").path(updateId.toString())
                .request(MediaType.APPLICATION_JSON).cookie("X-SessionId", sessionId.toString()).get(clazz);
    }

    // Get collection of versions
    public <U> U readVersions(final Long id, final Long sessionId, final GenericType<U> superType) {
        return service.path(uri).path(id.toString()).path("versions").request(MediaType.APPLICATION_JSON)
                .cookie("X-SessionId", sessionId.toString()).get(superType);
    }

    // update empty body with session
    public void update(final Long id, final Long sessionId) {
        service.path(uri).path(id.toString()).request(MediaType.APPLICATION_JSON)
                .cookie("X-SessionId", sessionId.toString()).put(Entity.json(null));
    }

    // create from non-canonical, serialized object, with session
    public T update(final Long id, final String instance, final MediaType mediaType, final Long sessionId) {
        return service.path(uri).path(id.toString()).request(MediaType.APPLICATION_JSON)
                .cookie("X-SessionId", sessionId.toString()).put(Entity.entity(instance, mediaType), clazz);
    }

    // update
    public T update(final Long id, final T entity) {
        return service.path(uri).path(id.toString()).request(MediaType.APPLICATION_JSON).put(Entity.json(entity),
                clazz);
    }

    // update with session
    public T update(final Long id, final T entity, final Long sessionId) {
        return service.path(uri).path(id.toString()).request(MediaType.APPLICATION_JSON)
                .cookie("X-SessionId", sessionId.toString()).put(Entity.json(entity), clazz);
    }

    // update with session and key
    public T update(final String key, final T entity, final Long sessionId) {
        return service.path(uri).path(key).request(MediaType.APPLICATION_JSON)
                .cookie("X-SessionId", sessionId.toString()).put(Entity.json(entity), clazz);
    }

    // update with session
    public T update(final T entity, final Long sessionId) {
        return service.path(uri).request(MediaType.APPLICATION_JSON).cookie("X-SessionId", sessionId.toString())
                .put(Entity.json(entity), clazz);
    }

    //
    // Update - multi-part form data
    // TODO: support multi-part
    // public T updateMultiPart(final Long id, final FormDataMultiPart formData, final Long sessionId) {
    // return service.path(uri).path(id.toString()).type(Boundary.addBoundary(MediaType.MULTIPART_FORM_DATA_TYPE))
    // .cookie("X-SessionId", sessionId.toString()).put(clazz, formData);
    // }

    public Boolean versionAvailable(final Long id, final Long updateId, final Long sessionId) {
        return service.path(uri).path(id.toString()).path("versions").path(updateId.toString()).path("available")
                .request().cookie("X-SessionId", sessionId.toString()).get(Boolean.class);
    }

    // overwrite with session and key
    public void updateNoResponse(final String key, final T entity, final Long sessionId) {
        service.path(uri).path(key).request(MediaType.APPLICATION_JSON).cookie("X-SessionId", sessionId.toString())
                .put(Entity.json(entity));
    }

    static private WebTarget applyQueryParams(WebTarget target, MultivaluedMap<String, String> queryParams) {
        WebTarget result = target;
        if (queryParams != null) {
            for (Entry<String, List<String>> entry : queryParams.entrySet()) {
                for (String value : entry.getValue()) {
                    result = result.queryParam(entry.getKey(), value);
                }
            }
        }
        return result;
    }
}
