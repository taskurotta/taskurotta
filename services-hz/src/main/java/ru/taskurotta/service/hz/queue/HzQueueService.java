package ru.taskurotta.service.hz.queue;

import com.hazelcast.core.DistributedObject;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IExecutorService;
import com.hazelcast.core.ILock;
import com.hazelcast.core.IMap;
import com.hazelcast.core.IQueue;
import com.hazelcast.core.Member;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.hazelcast.queue.delay.DelayIQueue;
import ru.taskurotta.hazelcast.queue.delay.QueueFactory;
import ru.taskurotta.hazelcast.util.ClusterUtils;
import ru.taskurotta.service.console.model.GenericPage;
import ru.taskurotta.service.console.model.QueueStatVO;
import ru.taskurotta.service.console.retriever.QueueInfoRetriever;
import ru.taskurotta.service.hz.console.HzQueueStatTask;
import ru.taskurotta.service.queue.QueueService;
import ru.taskurotta.service.queue.TaskQueueItem;
import ru.taskurotta.util.ActorUtils;
import ru.taskurotta.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Implementation of a QueueService for a Hazelcast cluster.
 * Date: 06.12.13 15:30
 */
public class HzQueueService implements QueueService, QueueInfoRetriever {

    private static final Logger logger = LoggerFactory.getLogger(HzQueueService.class);
    private transient final ReentrantLock lock = new ReentrantLock();
    private long pollDelay;
    protected final ConcurrentHashMap<String, Long> lastPolledTaskEnqueueTimes = new ConcurrentHashMap<>();

    protected QueueFactory queueFactory;
    protected HazelcastInstance hazelcastInstance;
    protected String queueNamePrefix;

    protected static final String HZ_QUEUE_INFO_EXECUTOR_SERVICE = "hzQueueInfoExecutorService";

    private static final String LAST_POLLED_TASK_ENQUEUE_TIME = "lastPolledTaskEnqueueTimes";
    private static final String SYNCH_LOCK_NAME = HzQueueService.class.getName().concat("#SINCH_LOCK");
    private static final String DRAIN_LOCK_NAME = HzQueueService.class.getName().concat("#DRAIN_LOCK");

    private ILock synchLock = null;

    private Map<String, DelayIQueue<TaskQueueItem>> queueMap = new ConcurrentHashMap<>();

    private ILock drainLock;

    private AtomicInteger cnt = new AtomicInteger(0);

    public HzQueueService(QueueFactory queueFactory, HazelcastInstance hazelcastInstance, String queueNamePrefix, long mergePeriodMs, long pollDelay) {
        this.queueFactory = queueFactory;
        this.hazelcastInstance = hazelcastInstance;
        this.queueNamePrefix = queueNamePrefix;
        this.pollDelay = pollDelay;

        this.synchLock = hazelcastInstance.getLock(SYNCH_LOCK_NAME);
        this.drainLock = hazelcastInstance.getLock(DRAIN_LOCK_NAME);

        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);

        //Queue statistics for recovery
        scheduledExecutorService.scheduleAtFixedRate(new StatisticsMerger(), 0, mergePeriodMs, TimeUnit.MILLISECONDS);
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

    class StatisticsMerger implements Runnable {

        @Override
        public void run() {

            synchLock.lock();

            try { //should always wrap ScheduledExecutorService tasks to try-catch to prevent silent task death

                IMap<String, Long> mutualMap = hazelcastInstance.getMap(LAST_POLLED_TASK_ENQUEUE_TIME);
                Map<String, Long> myMap = lastPolledTaskEnqueueTimes;

                Set<String> keys = new HashSet<>();
                keys.addAll(mutualMap.keySet());
                keys.addAll(myMap.keySet());

                for (String key: keys) {
                    Long mutualValue = mutualMap.get(key);
                    Long myValue = myMap.get(key);

                    if (mutualValue == null) {
                        mutualMap.set(key, myValue);

                    } else if (myValue == null) {
                        myMap.put(key, mutualValue);

                    } else if (myValue > mutualValue) {
                        mutualMap.set(key, myValue);

                    } else {
                        myMap.put(key, mutualValue);

                    }
                }

            } catch (Throwable e) {
                logger.error("StatisticsMerger iteration failed", e);
            } finally {
                synchLock.unlock();
            }
        }
    }

    @Override
    public long getLastPolledTaskEnqueueTime(String queueName) {
        Long time = lastPolledTaskEnqueueTimes.get(ActorUtils.toPrefixed(queueName, queueNamePrefix));

        // if no tasks in queue, than return -1
        if (time == null) {
            return -1;
        }

        return time;
    }

    @Override
    public void clearQueue(String queueName) {
        DelayIQueue<TaskQueueItem> queue = getQueue(ActorUtils.toPrefixed(queueName, queueNamePrefix));
        queue.clear();
    }

    @Override
    public void removeQueue(String queueName) {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            DelayIQueue<TaskQueueItem> queue = queueMap.get(ActorUtils.toPrefixed(queueName, queueNamePrefix));
            logger.debug("Removing queue with name [{}], cached queue is [{}]", queueName, queue);
            if (queue != null) {
                queueMap.remove(queueName);
                queue.destroy();
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public long getQueueStorageCount(String queueName) {
        DelayIQueue<TaskQueueItem> queue = queueMap.get(ActorUtils.toPrefixed(queueName, queueNamePrefix));
        return queue!=null? queue.getStorageSize(): -1;//-1 indicating that there is no such queue cached yet
    }

    @Override
    public TaskQueueItem poll(String actorId, String taskList) {

        String queueName = createQueueName(actorId, taskList);
        DelayIQueue<TaskQueueItem> queue = getQueue(queueName);

        TaskQueueItem result= null;
        try {
            result = queue.poll(pollDelay, TimeUnit.MILLISECONDS);
            if (logger.isDebugEnabled()) {
                logger.debug("poll() returns taskQueueItem [{}]. [{}].size: {}", result, queueName, queue.size());
            }

            if (result==null && queue.size()>0) {//possible queue store data loss -> apply null values drain
                result = getValueAfterNullDrain(queueName, queue);
                //do not update effective time if queue is nullified to prevent recovered tasks overflow
            } else {
                updateQueueEffectiveTime(queueName, result);
            }

        } catch (InterruptedException e) {
            logger.error("Queue poll operation interrupted", e);
        }

        return result;
    }

    private void updateQueueEffectiveTime (String queueName, TaskQueueItem result) {
        long lastPolledTaskEnqueueTime = (result!=null? result.getEnqueueTime() : System.currentTimeMillis());
        lastPolledTaskEnqueueTimes.put(queueName, lastPolledTaskEnqueueTime);
        logger.debug("lastPolledTaskEnqueueTimes updated for queue[{}] with new value [{}]", queueName, lastPolledTaskEnqueueTime);
    }

    private TaskQueueItem getValueAfterNullDrain(String queueName, DelayIQueue<TaskQueueItem> queue) {
        TaskQueueItem result = null;
        if (drainLock.tryLock()) {
            try {
                logger.warn("Null item polled from not empty queue: possible store data loss? Try to drain null values...");
                long cnt = 0l;
                long start = System.currentTimeMillis();
                do {
                    cnt++;
                    result = queue.poll();
                    if (cnt % 100 == 0) {
                        logger.warn("[{}] empty values drained away... Still processing", cnt);
                    }
                } while (result == null && queue.size()>0);
                logger.warn("Total: [{}] empty values drained away in [{}] ms", cnt, System.currentTimeMillis() - start);

            } finally {
                drainLock.unlock();
                updateQueueEffectiveTime(queueName, result);//update only when drain is finished
            }
        }
        return result;
    }

    @Override
    public boolean enqueueItem(String actorId, UUID taskId, UUID processId, long startTime, String taskList) {

        long now = System.currentTimeMillis();

        // set it to current time for precisely repeat
        if (startTime <= 0L) {
            startTime = now;
        }

        TaskQueueItem taskQueueItem = new TaskQueueItem();
        taskQueueItem.setTaskId(taskId);
        taskQueueItem.setProcessId(processId);
        taskQueueItem.setStartTime(startTime);
        taskQueueItem.setEnqueueTime(now);
        taskQueueItem.setTaskList(taskList);

        String queueName = createQueueName(actorId, taskList);
        DelayIQueue<TaskQueueItem> queue = getQueue(queueName);

        long delayTime = startTime - now;

        return queue.add(taskQueueItem, delayTime, TimeUnit.MILLISECONDS);
    }

    /**
     * Always return false, ignore this method's result.
     */
    @Override
    public boolean isTaskInQueue(String actorId, String taskList, UUID taskId, UUID processId) {
        return false;
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

    @Override
    public GenericPage<String> getQueueList(int pageNum, int pageSize) {

        List<String> queueNamesList = getTaskQueueNamesByPrefix(queueNamePrefix, null, false);

        logger.debug("Stored queue names for queue service are [{}]", queueNamesList);

        String[] queueNames = queueNamesList.toArray(new String[queueNamesList.size()]);
        List<String> result = new ArrayList<>(pageSize);

        if (queueNames.length > 0) {
            int pageStart = (pageNum - 1) * pageSize;
            int pageEnd = pageSize * pageNum >= queueNames.length ? queueNames.length : pageSize * pageNum;
            result.addAll(Arrays.asList(queueNames).subList(pageStart, pageEnd));
        }

        return new GenericPage<>(prefixStrip(result), pageNum, pageSize, queueNames.length);
    }

    @Override
    public int getQueueTaskCount(String queueName) {
        return getQueue(ActorUtils.toPrefixed(queueName, queueNamePrefix)).size();
    }

    @Override
    public GenericPage<TaskQueueItem> getQueueContent(String queueName, int pageNum, int pageSize) {
        List<TaskQueueItem> result = new ArrayList<>();

        DelayIQueue<TaskQueueItem> queue = getQueue(ActorUtils.toPrefixed(queueName, queueNamePrefix));
        TaskQueueItem[] queueItems = queue.toArray(new TaskQueueItem[queue.size()]);

        if (queueItems.length > 0) {
            int startIndex = (pageNum - 1) * pageSize;
            int endIndex = (pageSize * pageNum >= queueItems.length) ? queueItems.length : pageSize * pageNum;
            result.addAll(Arrays.asList(queueItems).subList(startIndex, endIndex));
        }

        return new GenericPage<>(result, pageNum, pageSize, queueItems.length);
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
            if (!queueNames.isEmpty()) {
                IExecutorService es = hazelcastInstance.getExecutorService(HZ_QUEUE_INFO_EXECUTOR_SERVICE);
                Map<Member, Future<List<QueueStatVO>>> results = es.submitToAllMembers(new HzQueueStatTask(new ArrayList<String>(queueNames), queueNamePrefix));
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
                        item.setLocal(ClusterUtils.isLocalQueue(queueNamePrefix+item.getName(), hazelcastInstance));
                    }

                    result = new GenericPage<>(resultItems, pageNum, pageSize, fullFilteredQueueNamesList.size());
                }
            }
        }

        return result;
    }

    @Override
    public List<String> getQueueNames() {
        return getTaskQueueNamesByPrefix(queueNamePrefix, null, false);
    }

    private DelayIQueue<TaskQueueItem> getQueue(String queueName) {

        DelayIQueue<TaskQueueItem> queue = queueMap.get(queueName);

        if (queue == null) {

            final ReentrantLock lock = this.lock;
            lock.lock();
            try {
                queue = queueMap.get(queueName);
                if (queue == null) {
                    queue = queueFactory.create(queueName);
                    queueMap.put(queueName, queue);
                }
            } finally {
                lock.unlock();
            }
        }

        return queue;
    }

    private List<String> getTaskQueueNamesByPrefix(String prefix, String filter, boolean prefixStrip) {
        List<String> result = new ArrayList<>();

        for (DistributedObject inst : hazelcastInstance.getDistributedObjects()) {
            if (inst instanceof IQueue) {
                String name = inst.getName();
                if (name.startsWith(prefix)) {
                    String item = prefixStrip ? name.substring(prefix.length()) : name;
                    if (StringUtils.isBlank(filter) || item.startsWith(filter)) {
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
}