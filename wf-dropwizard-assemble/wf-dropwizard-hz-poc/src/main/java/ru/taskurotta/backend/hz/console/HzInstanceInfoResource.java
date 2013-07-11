package ru.taskurotta.backend.hz.console;

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
    public HzInstanceInfoVO getInstanceInfo(@PathParam("instanceName")String instanceName, @PathParam("instanceType")String instanceType) {
        HzInstanceInfoVO result = null;
        for(Instance inst: hzInstance.getInstances()) {
            if (isInstanceOfInterest(instanceName, instanceType, inst)) {
                result = createResponse(inst);
                break;
            }
        }
        logger.debug("Instance info getted by type[{}] and name [{}] is [{}]", instanceType, instanceName, result);
        return result;
    }

    private HzInstanceInfoVO createResponse(Instance inst) {
        HzInstanceInfoVO result = new HzInstanceInfoVO();
        result.setId(String.valueOf(inst.getId()));
        result.setType(inst.getInstanceType().toString());

        InstanceType type = inst.getInstanceType();
        String name = null;
        int size = 0;

        if (type.isQueue()) {
            name = ((IQueue)inst).getName();
            size = ((IQueue)inst).size();
        } else if (type.isMap()) {
            name = ((IMap)inst).getName();
            size = ((IMap)inst).size();
        } else if (type.isMultiMap()) {
            name = ((MultiMap)inst).getName();
            size = ((MultiMap)inst).size();
        } else if (type.isSet()) {
            name = ((ISet)inst).getName();
            size = ((ISet)inst).size();
        } else if (type.isList()) {
            name = ((IList)inst).getName();
            size = ((IList)inst).size();
        }

        result.setName(name);
        result.setSize(size);

        return result;
    }

    private boolean isInstanceOfInterest(String nameOfInterest, String typeOfInterest, Instance targetInstance) {
        boolean result = false;
        String name = null;
        InstanceType type = targetInstance.getInstanceType();
        if (type.isQueue() && "queue".equalsIgnoreCase(typeOfInterest)) {
            name = ((IQueue)targetInstance).getName();
        } else if (type.isMap() && "map".equalsIgnoreCase(typeOfInterest)) {
            name = ((IMap)targetInstance).getName();
        } else if (type.isMultiMap() && "multimap".equalsIgnoreCase(typeOfInterest)) {
            name = ((MultiMap)targetInstance).getName();
        } else if (type.isSet() && "set".equalsIgnoreCase(typeOfInterest)) {
            name = ((ISet)targetInstance).getName();
        } else if (type.isList() && "list".equalsIgnoreCase(typeOfInterest)) {
            name = ((IList)targetInstance).getName();
        }
        result = nameOfInterest.equalsIgnoreCase(name);
        return result;
    }

    public void setHzInstance(HazelcastInstance hzInstance) {
        this.hzInstance = hzInstance;
    }
}
