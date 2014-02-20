package ru.taskurotta.test.mongofail;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;

import javax.ws.rs.core.MediaType;

/**
 * Date: 20.02.14 12:03
 */
public class RestRetriever implements FinishedCountRetriever {

    public static final String REST_SERVICE_PREFIX = "/rest/";

    private Client client = Client.create();

    private String endpoint;

    @Override
    public int getFinishedCount() {
        WebResource processesResource = client.resource(getContextUrl("/console/processes/finished/count"));
        WebResource.Builder rb = processesResource.getRequestBuilder();
        rb.type(MediaType.APPLICATION_JSON);
        rb.accept(MediaType.APPLICATION_JSON);

        return rb.get(Integer.class);
    }

    protected String getContextUrl(String path) {
        return endpoint.replaceAll("/*$", "") + REST_SERVICE_PREFIX + path.replaceAll("^/*", "");
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }
}
