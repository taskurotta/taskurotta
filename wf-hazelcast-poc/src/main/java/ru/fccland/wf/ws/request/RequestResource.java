package ru.fccland.wf.ws.request;

import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.yammer.metrics.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by void 15.03.13 18:51
 */
@Path("/request")
@Produces(MediaType.APPLICATION_JSON)
public class RequestResource {
	private static final Logger log = LoggerFactory.getLogger(RequestResource.class);

	private final AtomicLong counter;
	private Map<Long, Map<String, Object>> userSessions;

	public RequestResource() {
		counter = new AtomicLong(1);

		Config cfg = new Config();
		HazelcastInstance instance = Hazelcast.newHazelcastInstance(cfg);
		userSessions = instance.getMap("userSessions");
	}

	@GET
	@Timed
	@Path("/add")
	public RequestInfo add(@QueryParam("user") Long sessionId, @QueryParam("data") String data) {
		Map<String, Object> sessionData = userSessions.get(sessionId);
		if (null != sessionData) {
			return new RequestInfo(counter.incrementAndGet(), (String)sessionData.get("name"), data);
		}
		throw new WebApplicationException(Response.status(Response.Status.UNAUTHORIZED).entity("Access denied").build());
	}
}
