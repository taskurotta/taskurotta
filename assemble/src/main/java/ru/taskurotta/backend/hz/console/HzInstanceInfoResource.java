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
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * Created with IntelliJ IDEA.
 * User: dimadin
 * Date: 04.07.13 16:00
 */
@Path("/console/hz/{instanceType}/{instanceName}")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class HzInstanceInfoResource {

    private static final Logger logger = LoggerFactory.getLogger(HzInstanceInfoResource.class);
    private HazelcastInstance hzInstance;

    @GET
    public HzInstanceInfoVO getInstanceInfo(@PathParam("instanceName") String instanceName, @PathParam("instanceType") String instanceType) {
        HzInstanceInfoVO result = null;
        for (DistributedObject inst : hzInstance.getDistributedObjects()) {
            result = createResponse(inst);
        }
        logger.debug("Instance info getted by type[{}] and name [{}] is [{}]", instanceType, instanceName, result);
        return result;
    }

    private HzInstanceInfoVO createResponse(DistributedObject inst) {
        HzInstanceInfoVO result = new HzInstanceInfoVO();
        result.setId(String.valueOf(inst.getId()));

        String name = inst.getName();
        int size = 0;

        if (inst instanceof IQueue) {
            size = ((IQueue) inst).size();
        } else if (inst instanceof IMap) {
            size = ((IMap) inst).size();
        } else if (inst instanceof MultiMap) {
            size = ((MultiMap) inst).size();
        } else if (inst instanceof ISet) {
            size = ((ISet) inst).size();
        } else if (inst instanceof IList) {
            size = ((IList) inst).size();
        }

        result.setName(name);
        result.setSize(size);

        return result;
    }


    public void setHzInstance(HazelcastInstance hzInstance) {
        this.hzInstance = hzInstance;
    }
}
