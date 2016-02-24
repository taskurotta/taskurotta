package ru.taskurotta.e2e;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.util.Map;

@Path(TestResource.PATH)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.TEXT_PLAIN)
public class TestResource {

    private static final Logger logger = LoggerFactory.getLogger(TestResource.class);

    public static final String PATH = "/e2e";
    public static final String INIT_METHOD = "/init";
    public static final String CLEAN_METHOD = "/clean";

    Map<String, SpecSuite> specSuits;

    public TestResource(Map<String, SpecSuite> specSuits) {
        this.specSuits = specSuits;

        for (String specSuiteName: specSuits.keySet()) {
            logger.info("Spec suite [{}] is registered", specSuiteName);
        }
    }

    @GET
    @Path(INIT_METHOD)
    public String init(@QueryParam("name") String name) {
        SpecSuite specSuite = specSuits.get(name);

        if (specSuite == null) {
            throw new IllegalArgumentException(String.format("Spec suite '%s' not found", name));
        }

        specSuite.init();
        return String.format("Init '%s' is executed", name);
    }

    @GET
    @Path(CLEAN_METHOD)
    public String clean(@QueryParam("name") String name) {

        return String.format("Clean '%s' is executed", name);
    }


}
