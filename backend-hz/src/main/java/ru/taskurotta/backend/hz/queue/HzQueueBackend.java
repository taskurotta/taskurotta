package ru.taskurotta.backend.hz.queue;

import com.hazelcast.core.DistributedObject;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.core.IQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import ru.taskurotta.backend.console.model.GenericPage;
import ru.taskurotta.backend.console.retriever.QueueInfoRetriever;
import ru.taskurotta.backend.hz.support.HzMapConfigSpringSupport;
import ru.taskurotta.backend.hz.support.HzQueueSpringConfigSupport;
import ru.taskurotta.backend.queue.QueueBackend;
import ru.taskurotta.backend.queue.TaskQueueItem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

/**
 * Created by void, dudin 07.06.13 11:00
 */
public class HzQueueBackend implements QueueBackend, QueueInfoRetriever {

    private final static Logger logger = LoggerFactory.getLogger(HzQueueBackend.class);

    private final static String DELAYED_TASKS_LOCK = "DELAYED_TASKS_LOCK";

    private int pollDelay = 60;
    private TimeUnit pollDelayUnit = TimeUnit.SECONDS;
    private String queueNamePrefix;

    //Hazelcast specific
    private HazelcastInstance hazelcastInstance;
    private Lock delayedTasksLock;

    private Map<String, IQueue<TaskQueueItem>> hzQueues = new ConcurrentHashMap<>();
    private Map<String, IMap<UUID, TaskQueueItem>> hzDelayedQueues = new ConcurrentHashMap<>();

    private HzQueueSpringConfigSupport hzQueueConfigSupport;
    private HzMapConfigSpringSupport hzMapConfigSpringSupport;

    private MongoTemplate mongoTemplate;

    public void setHzQueueConfigSupport(HzQueueSpringConfigSupport hzQueueConfigSupport) {
        this.hzQueueConfigSupport = hzQueueConfigSupport;
    }

    public HzQueueBackend(int pollDelay, TimeUnit pollDelayUnit, HazelcastInstance hazelcastInstance) {
        this.hazelcastInstance = hazelcastInstance;
        this.pollDelay = pollDelay;
        this.pollDelayUnit = pollDelayUnit;

        delayedTasksLock = hazelcastInstance.getLock(DELAYED_TASKS_LOCK);
    }

    @Override
    public GenericPage<String> getQueueList(int pageNum, int pageSize) {
        List<String> queueNamesList = getTaskQueueNamesByPrefix();

        logger.debug("Stored queue names for queue backend are [{}]", queueNamesList);

        String[] queueNames = queueNamesList.toArray(new String[queueNamesList.size()]);
        List<String> result = new ArrayList<>(pageSize);
        if (queueNames.length > 0) {
            int pageStart = (pageNum - 1) * pageSize;
            int pageEnd = pageSize * pageNum >= queueNames.length ? queueNames.length : pageSize * pageNum;
            result.addAll(Arrays.asList(queueNames).subList(pageStart, pageEnd));
        }
        return new GenericPage<>(prefixStrip(result), pageNum, pageSize, queueNames.length);
    }

    private List<String> getTaskQueueNamesByPrefix() {
        List<String> result = new ArrayList<>();
        for (DistributedObject inst : hazelcastInstance.getDistributedObjects()) {
            if (inst instanceof IQueue) {
                String name = inst.getName();
                if (name.startsWith(queueNamePrefix)) {
                    result.add(name);
                }
            }
        }
        return result;
    }

    private List<String> prefixStrip(List<String> target) {
        if (queueNamePrefix == null) {
            return target;
        }
        List<String> result = null;
        if (target != null && !target.isEmpty()) {
            result = new ArrayList<>();
            for (String item : target) {
                result.add(item.substring(queueNamePrefix.length()));
            }
        }
        return result;
    }

    @Override
    public int getQueueTaskCount(String queueName) {
        if (queueNamePrefix != null && !queueName.startsWith(queueNamePrefix)) {
            queueName = queueNamePrefix + queueName;
        }
        return getHzQueue(queueName).size();
    }

    @Override
    public GenericPage<TaskQueueItem> getQueueContent(String queueName, int pageNum, int pageSize) {
        if (queueNamePrefix != null && !queueName.startsWith(queueNamePrefix)) {
            queueName = queueNamePrefix + queueName;
        }
        List<TaskQueueItem> result = new ArrayList<>();
        IQueue<TaskQueueItem> queue = getHzQueue(queueName);
        TaskQueueItem[] queueItems = queue.toArray(new TaskQueueItem[queue.size()]);

        if (queueItems.length > 0) {
            int startIndex = (pageNum - 1) * pageSize;
            int endIndex = (pageSize * pageNum >= queueItems.length) ? queueItems.length : pageSize * pageNum;
            result.addAll(Arrays.asList(queueItems).subList(startIndex, endIndex));
        }
        return new GenericPage<>(result, pageNum, pageSize, queueItems.length);
    }

    @Override
    public TaskQueueItem poll(String actorId, String taskList) {
        String queueName = createQueueName(actorId, taskList);
        IQueue<TaskQueueItem> queue = getHzQueue(queueName);

        TaskQueueItem result = null;
        try {
            result = queue.poll(pollDelay, pollDelayUnit);
        } catch (InterruptedException e) {
            logger.error("Thread was interrupted at poll, releasing it", e);
        }

        if (logger.isDebugEnabled()) {
            logger.debug("poll() returns taskId [{}]. Queue.size: {}", result, queue.size());
        }

        return result;
    }

    /**
     * This is a cache proxy of hazelcastInstance.getQueue invocations
     *
     * @param queueName
     * @return
     */
    private IQueue<TaskQueueItem> getHzQueue(String queueName) {
        IQueue<TaskQueueItem> queue = hzQueues.get(queueName);

        if (queue != null) {
            return queue;
        }

        synchronized (hzQueues) {
            queue = hzQueues.get(queueName);
            if (queue != null) {
                return queue;
            }
            if (hzQueueConfigSupport != null) {
                hzQueueConfigSupport.createQueueConfig(queueName);
            } else {
                logger.warn("WARNING: hzQueueConfigSupport implementation is not set to HzQueueBackend, queues are not persistent!");
            }

            queue = hazelcastInstance.getQueue(queueName);//never null
            hzQueues.put(queueName, queue);
        }

        return queue;
    }

    /**
     * This is a cache proxy of hazelcastInstance.getQueue invocations
     */
    private IMap<UUID, TaskQueueItem> getHzDelayedMap(String queueName) {
        IMap<UUID, TaskQueueItem> map = hzDelayedQueues.get(queueName);

        if (map != null) {
            return map;
        }
        synchronized (hzDelayedQueues) {
            map = hzDelayedQueues.get(queueName);
            if (map != null) {
                return map;
            }
            if (hzMapConfigSpringSupport != null) {
                hzMapConfigSpringSupport.createMapConfig(queueName);
            } else {
                logger.warn("WARNING: hzMapConfigSpringSupport implementation is not set to HzQueueBackend, delayed queues are not persistent!");
            }

            map = hazelcastInstance.getMap(queueName);
            map.addIndex("startTime", true);
            hzDelayedQueues.put(queueName, map);
        }

        return map;
    }

    @Override
    public void enqueueItem(String actorId, UUID taskId, UUID processId, long startTime, String taskList) {

        // set it to current time for precisely repeat
        if (startTime <= 0L) {
            startTime = System.currentTimeMillis();
        }

        TaskQueueItem item = new TaskQueueItem();
        item.setTaskId(taskId);
        item.setProcessId(processId);
        item.setStartTime(startTime);
        item.setEnqueueTime(System.currentTimeMillis());
        item.setTaskList(taskList);

        if (item.getStartTime() <= item.getEnqueueTime()) {

            IQueue<TaskQueueItem> queue = getHzQueue(createQueueName(actorId, taskList));
            try {
                queue.add(item);
            } catch (Exception ex) {
                logger.error("", ex);
                logger.warn(queue.getLocalQueueStats().toString());
            }

            if (logger.isDebugEnabled()) {
                logger.debug("enqueue item [actorId [{}], taskId [{}], startTime [{}]; Queue.size: {}]", actorId, taskId, startTime, queue.size());
            }

        } else {

            String mapName = getDelayedTasksMapName(createQueueName(actorId, taskList));
            IMap<UUID, TaskQueueItem> map = getHzDelayedMap(mapName);
            map.set(taskId, item, 0, TimeUnit.SECONDS);

            if (logger.isDebugEnabled()) {
                logger.debug("Add to waiting set item [actorId [{}], taskId [{}], startTime [{}]; Set.size: {}]", actorId, taskId, startTime, map.size());
            }

        }

    }

    @Override
    public boolean isTaskInQueue(String actorId, String taskList, UUID taskId, UUID processId) {
        String queueName = createQueueName(actorId, taskList);

        TaskQueueItem taskQueueItem = new TaskQueueItem();
        taskQueueItem.setProcessId(processId);
        taskQueueItem.setTaskId(taskId);
        taskQueueItem.setQueueName(queueName);
        taskQueueItem.setTaskList(taskList);

        IQueue<TaskQueueItem> queue = getHzQueue(queueName);

        return queue.contains(taskQueueItem);
    }

    @Override
    public Map<String, Integer> getHoveringCount(float periodSize) {
        return null;
    }

    @Override
    public String createQueueName(String actorId, String taskList) {
        StringBuilder result = new StringBuilder(null != queueNamePrefix ? queueNamePrefix : "");

        result.append(actorId);
        if (taskList != null) {
            result.append("#").append(taskList);
        }

        return result.toString();
    }

    private String getDelayedTasksMapName(String queueName) {
        return "delayed." + queueName;
    }

    public void setQueueNamePrefix(String queueNamePrefix) {
        this.queueNamePrefix = queueNamePrefix;
    }

    public void updateDelayedTasks() {
        if (!delayedTasksLock.tryLock()) {
            logger.debug("Can't get lock for delayed tasks. Other member will do this");
            return;
        }
        long startTime = System.nanoTime();
        try {
            List<String> queueNamesList = getTaskQueueNamesByPrefix();
            if (logger.isDebugEnabled()) {
                logger.debug("Start update delayed tasks for queues: {}", queueNamesList);
            }

            for (String queueName : queueNamesList) {
                String mapName = getDelayedTasksMapName(queueName);
                IMap<UUID, TaskQueueItem> waitingItems = getHzDelayedMap(mapName);
                IQueue<TaskQueueItem> queue = getHzQueue(queueName);

                List<TaskQueueItem> readyItemList = mongoTemplate.find(Query.query(Criteria.where("startTime").lt(System.currentTimeMillis())), TaskQueueItem.class, mapName);

                if (logger.isDebugEnabled()) {
                    long endTime = System.nanoTime();
                    logger.debug("search time (in mongo): {}s; {} ready items for queue [{}], mapSize [{}]", String.format("%8.3f", (endTime - startTime) / 1e9), readyItemList.size(), queueName, waitingItems.size());
                }

                for (TaskQueueItem next : readyItemList) {
                    queue.add(next);
                    waitingItems.remove(next.getTaskId());
                }

            }
            long endTime = System.nanoTime();

            if (logger.isDebugEnabled()) {
                logger.debug("spent time (total): {}s", String.format("%8.3f", (endTime - startTime) / 1e9));
            }
        } finally {
            delayedTasksLock.unlock();
        }
    }

    public HzMapConfigSpringSupport getHzMapConfigSpringSupport() {
        return hzMapConfigSpringSupport;
    }

    public void setHzMapConfigSpringSupport(HzMapConfigSpringSupport hzMapConfigSpringSupport) {
        this.hzMapConfigSpringSupport = hzMapConfigSpringSupport;
    }

    public HzQueueSpringConfigSupport getHzQueueConfigSupport() {
        return hzQueueConfigSupport;
    }
}
