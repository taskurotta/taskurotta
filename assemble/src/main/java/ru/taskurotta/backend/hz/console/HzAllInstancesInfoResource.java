package ru.taskurotta.backend.hz.console;

import com.hazelcast.core.DistributedObject;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IList;
import com.hazelcast.core.IMap;
import com.hazelcast.core.IQueue;
import com.hazelcast.core.ISet;
import com.hazelcast.core.MultiMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

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

        for (DistributedObject inst : hzInstance.getDistributedObjects()) {

            if (inst instanceof IQueue) {
                String name = inst.getName();
                result.getQueueNames().add(name);
            } else if (inst instanceof IMap) {
                String name = inst.getName();
                result.getMapNames().add(name);
            } else if (inst instanceof MultiMap) {
                String name = inst.getName();
                result.getMultimapNames().add(name);
            } else if (inst instanceof ISet) {
                String name = inst.getName();
                result.getSetNames().add(name);
            } else if (inst instanceof IList) {
                String name = inst.getName();
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
