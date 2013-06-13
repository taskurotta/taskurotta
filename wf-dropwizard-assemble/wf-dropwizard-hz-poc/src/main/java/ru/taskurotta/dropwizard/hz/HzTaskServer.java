package ru.taskurotta.dropwizard.hz;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.core.IQueue;
import com.hazelcast.core.ISet;
import com.hazelcast.core.Member;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;
import ru.taskurotta.backend.BackendBundle;
import ru.taskurotta.backend.config.ConfigBackend;
import ru.taskurotta.backend.dependency.DependencyBackend;
import ru.taskurotta.backend.queue.QueueBackend;
import ru.taskurotta.backend.storage.ProcessBackend;
import ru.taskurotta.backend.storage.TaskBackend;
import ru.taskurotta.server.GeneralTaskServer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * General task server with injected Hazelcast entities
 * Test purpose
 * User: dimadin
 * Date: 06.06.13 15:55
 *
 */
@Deprecated
public class HzTaskServer extends GeneralTaskServer {

    private static final Logger logger = LoggerFactory.getLogger(HzTaskServer.class);

    protected HazelcastInstance hzInstance;

    protected IMap hzMap;

    protected IQueue<String> hzQueue;

    protected ISet<String> hzSet;

    public HzTaskServer(BackendBundle backendBundle) {
        super(backendBundle);
    }

    public HzTaskServer(ProcessBackend processBackend, TaskBackend taskBackend, QueueBackend queueBackend, DependencyBackend dependencyBackend, ConfigBackend configBackend) {
        super(processBackend, taskBackend, queueBackend, dependencyBackend, configBackend);
    }

    public void init() {
        logger.info("Initialize task server with hazelcast instance name [{}], networkConfig[{}] ", hzInstance.getName(), hzInstance.getConfig().getNetworkConfig());

        Set<Member> members = hzInstance.getCluster().getMembers();
        logger.info("Hazelcast members are [{}]", members);

        String nameKey = "Name";
        if(members.size() == 1) {//first instance
            List<String> value = new ArrayList<String>();
            value.add("Mary");
            value.add("Harry");
            value.add("Jerry");
            hzMap.put(nameKey, value);
            logger.info("Added values[{}] to shared map", value);

            String[] martinis = new String[]{"Rosso", "Bianco", "Rosato", "D’Oro", "Fiero", "Rosso", "Bianco"};
            logger.info("Try to add to set[{}] and enqueue to queue[{}] values [{}]", hzSet.getName(), hzQueue.getName(), Arrays.asList(martinis));
            for(String martini: martinis) {
                if(!hzQueue.offer(martini)){
                    logger.info("Failed to enqueue item[{}]", martini);
                }
                if(!hzSet.add(martini)) {
                    logger.info("Value already present in set[{}]", Arrays.asList(martini));
                }
            }
        }

        logger.info("Map contents: [{}]", hzMap.get(nameKey));
        logger.info("Set content is: [{}]", new ArrayList(hzSet));
        logger.info("Two polled values from queue are: [{}]", Arrays.asList(new String[]{hzQueue.poll(), hzQueue.poll()}));

    }

    @Required
    public void setHzInstance(HazelcastInstance hzInstance) {
        this.hzInstance = hzInstance;
    }

    @Required
    public void setHzMap(IMap hzMap) {
        this.hzMap = hzMap;
    }

    @Required
    public void setHzQueue(IQueue hzQueue) {
        this.hzQueue = hzQueue;
    }

    @Required
    public void setHzSet(ISet hzSet) {
        this.hzSet = hzSet;
    }

}
