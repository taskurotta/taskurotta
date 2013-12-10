package ru.taskurotta.schedule.console;

import com.google.common.base.Optional;
import org.quartz.CronExpression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.text.ParseException;

/**
 * User: dimadin
 * Date: 26.09.13 10:37
 */
@Path("/console/schedule/validate/{type}")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class SchedulerValidationResource {

    private static final Logger logger = LoggerFactory.getLogger(SchedulerValidationResource.class);

    public static final String TYPE_CRON = "cron";

    @GET
    public Response validate(@PathParam("type") String type, @QueryParam("value") Optional<String> valueOpt) {
        String value = valueOpt.or("");
        if (TYPE_CRON.equalsIgnoreCase(type)) {

            try {
                CronExpression.validateExpression(value);
                return Response.ok().build();
            } catch (ParseException e) {
                logger.debug("Error parsing cron expression [{}]", value);
                return Response.ok("Invalid cron", MediaType.APPLICATION_JSON).build();
            }

        } else {
            logger.error("Unsupported type ["+type+"]");
            return Response.serverError().build();
        }
    }


}
