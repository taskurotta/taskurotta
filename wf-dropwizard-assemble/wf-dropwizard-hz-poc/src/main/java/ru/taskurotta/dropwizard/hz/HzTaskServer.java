package ru.taskurotta.dropwizard.hz;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.core.IQueue;
import com.hazelcast.core.ISet;
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
import java.util.List;

/**
 * General task server with injected Hazelcast entities
 * User: dimadin
 * Date: 06.06.13 15:55
 */
public class HzTaskServer extends GeneralTaskServer {

    private static final Logger logger = LoggerFactory.getLogger(HzTaskServer.class);

    protected HazelcastInstance hzInstance;

    protected IMap hzMap;

    protected IQueue<String> hzQueue;

    protected ISet<String> hzSet;

    protected int hzPort;

    public HzTaskServer(BackendBundle backendBundle) {
        super(backendBundle);
    }

    public HzTaskServer(ProcessBackend processBackend, TaskBackend taskBackend, QueueBackend queueBackend, DependencyBackend dependencyBackend, ConfigBackend configBackend) {
        super(processBackend, taskBackend, queueBackend, dependencyBackend, configBackend);
    }

    public void init() {
        logger.info("Initialize task server with hazelcast instance name [{}], networkConfig[{}] ", hzInstance.getName(), hzInstance.getConfig().getNetworkConfig());

        int port = hzInstance.getConfig().getNetworkConfig().getPort();
        String nameKey = "Name";

        if(port == hzPort) {//first instance (with not incremented default port)
            List<String> value = new ArrayList<String>();
            value.add("Mary");
            value.add("Harry");
            value.add("Jerry");
            hzMap.put(nameKey, value);
            logger.info("Added values[{}] to shared map", value);

            String[] martinis = new String[]{"Rosso", "Bianco", "Rosato", "D’Oro", "Fiero", "Rosso", "Bianco"};
            for(String martini: martinis) {
                logger.info("Try to add to set and enqueue value [{}]", martini);
                if(!hzQueue.offer(martini)){
                    logger.info("Failed to enqueue item[{}]", martini);
                }
                if(!hzSet.add(martini)) {
                    logger.info("Value already present in set[{}]", martini);
                }
            }
        }

        logger.info("Map contents: [{}]", hzMap.get(nameKey));
        logger.info("Set content is: [{}]", hzSet.toArray());
        logger.info("Two polled values from queue are: [{}]", new String[]{hzQueue.poll(), hzQueue.poll()});

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

    @Required
    public void setHzPort(int hzPort) {
        this.hzPort = hzPort;
    }
}
