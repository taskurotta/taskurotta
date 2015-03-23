package ru.taskurotta.dropwizard.resources.console.meta;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Provides information from jar inner manifest file entries
 * Date: 18.12.13 18:33
 */
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path("/console/manifest")
public class ManifestInfoResource {

    private static final Logger logger = LoggerFactory.getLogger(ManifestInfoResource.class);

    private final String version = "\"" + getManifestVersion() + "\"";

    @GET
    @Path("/version")
    public String getImplementationVersion() {
        logger.debug("Implementation version is [{}]", version);
        return version;
    }

    protected String getManifestVersion() {
        String result = "n/a";
        Properties allProps = getManifestProperties();
        logger.debug("Manifest file properties are [{}]", allProps);
        if (allProps!=null && allProps.containsKey("Implementation-Version")) {
            result = allProps.getProperty("Implementation-Version");
        }
        logger.debug("Implementation version is [{}]", result);
        return result;
    }

    protected Properties getManifestProperties() {
        Properties result = new Properties();

        try (InputStream stream = getClass().getClassLoader().getResourceAsStream("META-INF/MANIFEST.MF");) {
            if (stream != null) {
                result.load(stream);
            }  else {
                logger.error("File [META-INF/MANIFEST.MF] was not found");
            }
        } catch (IOException e) {
            logger.error("Cannot read manifest file properties", e);
        }

        return result;
    }

}
