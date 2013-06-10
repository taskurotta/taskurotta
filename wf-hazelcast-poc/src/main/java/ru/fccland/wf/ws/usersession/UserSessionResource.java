package ru.fccland.wf.ws.usersession;

import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.yammer.metrics.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by void 15.03.13 13:15
 */
@Path("/user-session")
@Produces(MediaType.APPLICATION_JSON)
public class UserSessionResource {
	private static final Logger log = LoggerFactory.getLogger(UserSessionResource.class);

	private final AtomicLong counter;
	private Map<Long, Map<String, Object>> userSessions;

	public UserSessionResource() {
		counter = new AtomicLong(1);

		Config cfg = new Config();
		HazelcastInstance instance = Hazelcast.newHazelcastInstance(cfg);
		userSessions = instance.getMap("userSessions");
	}

	@GET
	@Timed
	@Path("/login")
	public SessionInfo login(@QueryParam("name") String name, @QueryParam("pass") String pass) {
		log.info("login {} called", name);
		if (StringUtils.hasText(name)) {
			Map<String, Object> session = new HashMap<String, Object>();
			session.put("name", name);
			session.put("pass", pass);
			long id  = counter.incrementAndGet();
			userSessions.put(id, session);
			return new SessionInfo(id, "Ok");
		} else {
			return new SessionInfo(-1L, "Access denied");
		}
	}

	@GET
	@Timed
	@Path("/logout/{id}")
	public SessionInfo logout(@PathParam("id") Long id) {
		log.info("logout {} called", id);
		Map<String, Object> session = userSessions.remove(id);
		if (null != session) {
			return new SessionInfo(id, "Successfully logged out");
		}
		return new SessionInfo(id, "Session not found");
	}
}
