package ru.taskurotta.backend.hz.console;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IList;
import com.hazelcast.core.IMap;
import com.hazelcast.core.IQueue;
import com.hazelcast.core.ISet;
import com.hazelcast.core.Instance;
import com.hazelcast.core.Instance.InstanceType;
import com.hazelcast.core.MultiMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created with IntelliJ IDEA.
 * User: dimadin
 * Date: 04.07.13 15:52
 */
@Path("/console/hz")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class HzAllInstancesInfoResource {

    private static final Logger logger = LoggerFactory.getLogger(HzAllInstancesInfoResource.class);
    private HazelcastInstance hzInstance;

    @GET
    public HzInfoVO getHazelcastInfo() {
        HzInfoVO result = new HzInfoVO();

        for (Instance inst : hzInstance.getInstances()) {
            InstanceType type = inst.getInstanceType();
            if (type.isQueue()) {
                String name = ((IQueue) inst).getName();
                result.getQueueNames().add(name);
            } else if (type.isMap()) {
                String name = ((IMap) inst).getName();
                result.getMapNames().add(name);
            } else if (type.isMultiMap()) {
                String name = ((MultiMap) inst).getName();
                result.getMultimapNames().add(name);
            } else if (type.isSet()) {
                String name = ((ISet) inst).getName();
                result.getSetNames().add(name);
            } else if (type.isList()) {
                String name = ((IList) inst).getName();
                result.getListNames().add(name);
            }
        }

        logger.debug("hazelcast instances info getted is [{}]", result);

        return result;
    }

    public void setHzInstance(HazelcastInstance hzInstance) {
        this.hzInstance = hzInstance;
    }

}
