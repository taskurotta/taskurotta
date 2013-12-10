package ru.taskurotta.backend.hz.queue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

import com.hazelcast.core.DistributedObject;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IExecutorService;
import com.hazelcast.core.IMap;
import com.hazelcast.core.IQueue;
import com.hazelcast.core.Member;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import ru.taskurotta.backend.console.model.GenericPage;
import ru.taskurotta.backend.console.model.QueueStatVO;
import ru.taskurotta.backend.console.retriever.QueueInfoRetriever;
import ru.taskurotta.backend.hz.console.HzQueueStatTask;
import ru.taskurotta.backend.queue.QueueBackend;
import ru.taskurotta.backend.queue.TaskQueueItem;
import ru.taskurotta.hazelcast.HzMapConfigSpringSupport;
import ru.taskurotta.hazelcast.HzQueueSpringConfigSupport;
import ru.taskurotta.util.ActorUtils;
import ru.taskurotta.util.StringUtils;

/**
 * Created by void, dudin 07.06.13 11:00
 */
public class HzMongoQueueBackend implements QueueBackend, QueueInfoRetriever {

    private final static Logger logger = LoggerFactory.getLogger(HzMongoQueueBackend.class);

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

    private static final String HZ_QUEUE_INFO_EXECUTOR_SERVICE = "hzQueueInfoExecutorService";

    public void setHzQueueConfigSupport(HzQueueSpringConfigSupport hzQueueConfigSupport) {
        this.hzQueueConfigSupport = hzQueueConfigSupport;
    }

    public HzMongoQueueBackend(int pollDelay, TimeUnit pollDelayUnit, HazelcastInstance hazelcastInstance) {
        this.hazelcastInstance = hazelcastInstance;
        this.pollDelay = pollDelay;
        this.pollDelayUnit = pollDelayUnit;

        delayedTasksLock = hazelcastInstance.getLock(DELAYED_TASKS_LOCK);
    }

    @Override
    public GenericPage<String> getQueueList(int pageNum, int pageSize) {
        List<String> queueNamesList = getTaskQueueNamesByPrefix(queueNamePrefix, null, false);

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

    private List<String> getTaskQueueNamesByPrefix(String prefix, String filter, boolean prefixStrip) {
        List<String> result = new ArrayList<>();
        for (DistributedObject inst : hazelcastInstance.getDistributedObjects()) {
            if (inst instanceof IQueue) {
                String name = inst.getName();
                if (name.startsWith(prefix)) {
                    String item = prefixStrip ? name.substring(prefix.length()) : name;
                    if (StringUtils.isBlank(filter)
                            || item.startsWith(filter)) {
                        result.add(item);
                    }
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
        return getHzQueue(ActorUtils.toPrefixed(queueName, queueNamePrefix)).size();
    }

    @Override
    public GenericPage<TaskQueueItem> getQueueContent(String queueName, int pageNum, int pageSize) {
        List<TaskQueueItem> result = new ArrayList<>();
        IQueue<TaskQueueItem> queue = getHzQueue(ActorUtils.toPrefixed(queueName, queueNamePrefix));
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
                logger.warn("WARNING: hzQueueConfigSupport implementation is not set to HzMongoQueueBackend, queues are not persistent!");
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
                logger.warn("WARNING: hzMapConfigSpringSupport implementation is not set to HzMongoQueueBackend, delayed queues are not persistent!");
            }

            map = hazelcastInstance.getMap(queueName);
            map.addIndex("startTime", true);
            hzDelayedQueues.put(queueName, map);
        }

        return map;
    }

    @Override
    public boolean enqueueItem(String actorId, UUID taskId, UUID processId, long startTime, String taskList) {

        boolean result = false;

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
                result = queue.add(item);
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

            result = true;
        }

        return result;
    }

    /**
     * Always return false, ignore this method's result.
     */
    @Override
    public boolean isTaskInQueue(String actorId, String taskList, UUID taskId, UUID processId) {
        return false;
    }

    @Override
    public Map<String, Integer> getHoveringCount(float periodSize) {
        return null;
    }

    @Override
    public GenericPage<QueueStatVO> getQueuesStatsPage(int pageNum, int pageSize, String filter) {
        GenericPage<QueueStatVO> result = null;
        List<String> fullFilteredQueueNamesList = getTaskQueueNamesByPrefix(queueNamePrefix, filter, true);

        if (fullFilteredQueueNamesList != null && !fullFilteredQueueNamesList.isEmpty()) {
            int pageStart = (pageNum - 1) * pageSize;
            int pageEnd = Math.min(pageSize * pageNum, fullFilteredQueueNamesList.size());

            List<String> queueNames = fullFilteredQueueNamesList.subList(pageStart, pageEnd);
            if (queueNames != null && !queueNames.isEmpty()) {
                IExecutorService es = hazelcastInstance.getExecutorService(HZ_QUEUE_INFO_EXECUTOR_SERVICE);
                Map<Member, Future<List<QueueStatVO>>> results = es.submitToAllMembers(new HzQueueStatTask(queueNames, queueNamePrefix));
                List<QueueStatVO> resultItems = new ArrayList<>();
                int nodes = 0;
                for (Future<List<QueueStatVO>> nodeResultFuture : results.values()) {
                    try {
                        mergeByQueueName(resultItems, nodeResultFuture.get(5, TimeUnit.SECONDS));
                        nodes++;
                    } catch (Exception e) {
                        logger.warn("Cannot obtain QueueStatVO data from node", e);
                    }
                }

                if (!resultItems.isEmpty()) {
                    for (QueueStatVO item : resultItems) {
                        item.setNodes(nodes);
                    }
                    result = new GenericPage<QueueStatVO>(resultItems, pageNum, pageSize, fullFilteredQueueNamesList.size());
                }

            }

        }

        return result;
    }


    private void mergeByQueueName(List<QueueStatVO> mergeTo, List<QueueStatVO> mergeFrom) {
        if (mergeFrom != null && !mergeFrom.isEmpty()) {
            for (QueueStatVO mergeFromItem : mergeFrom) {
                QueueStatVO mergeTarget = getItemByName(mergeTo, mergeFromItem.getName());
                if (mergeTarget != null) {
                    mergeTarget.sumValuesWith(mergeFromItem);
                } else {
                    mergeTo.add(mergeFromItem);
                }
            }
        }
    }

    private static QueueStatVO getItemByName(List<QueueStatVO> list, String name) {
        QueueStatVO result = null;
        if (list != null && !list.isEmpty() && !StringUtils.isBlank(name)) {
            for (QueueStatVO qs : list) {
                if (name.equals(qs.getName())) {
                    result = qs;
                    break;
                }
            }
        }
        return result;
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
            List<String> queueNamesList = getTaskQueueNamesByPrefix(queueNamePrefix, null, false);
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
