package ru.taskurotta.backend.hz.queue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import com.hazelcast.core.DistributedObject;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IExecutorService;
import com.hazelcast.core.IQueue;
import com.hazelcast.core.Member;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.backend.console.model.GenericPage;
import ru.taskurotta.backend.console.model.QueueStatVO;
import ru.taskurotta.backend.console.retriever.QueueInfoRetriever;
import ru.taskurotta.backend.hz.console.HzQueueStatTask;
import ru.taskurotta.backend.hz.queue.delay.DelayIQueue;
import ru.taskurotta.backend.hz.queue.delay.QueueFactory;
import ru.taskurotta.backend.queue.QueueBackend;
import ru.taskurotta.backend.queue.TaskQueueItem;
import ru.taskurotta.util.ActorUtils;
import ru.taskurotta.util.StringUtils;

/**
 * User: stukushin
 * Date: 06.12.13
 * Time: 15:30
 */
public class HzQueueBackend implements QueueBackend, QueueInfoRetriever {

    private static final Logger logger = LoggerFactory.getLogger(HzQueueBackend.class);

    private QueueFactory queueFactory;
    private HazelcastInstance hazelcastInstance;
    private String queueNamePrefix;

    private static final String HZ_QUEUE_INFO_EXECUTOR_SERVICE = "hzQueueInfoExecutorService";

    private transient final ReentrantLock lock = new ReentrantLock();
    private Map<String, DelayIQueue<TaskQueueItem>> queueMap = new ConcurrentHashMap<>();

    public HzQueueBackend(QueueFactory queueFactory, HazelcastInstance hazelcastInstance, String queueNamePrefix) {
        this.queueFactory = queueFactory;
        this.hazelcastInstance = hazelcastInstance;
        this.queueNamePrefix = queueNamePrefix;
    }

    @Override
    public TaskQueueItem poll(String actorId, String taskList) {

        String queueName = createQueueName(actorId, taskList);
        DelayIQueue<TaskQueueItem> queue = getQueue(queueName);

        TaskQueueItem taskQueueItem = queue.poll();
        if (logger.isDebugEnabled()) {
            logger.debug("poll() returns taskQueueItem [{}]. [{}].size: {}", taskQueueItem, queueName, queue.size());
        }

        return taskQueueItem;
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
                    }

                    result = new GenericPage<>(resultItems, pageNum, pageSize, fullFilteredQueueNamesList.size());
                }
            }
        }

        return result;
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
}