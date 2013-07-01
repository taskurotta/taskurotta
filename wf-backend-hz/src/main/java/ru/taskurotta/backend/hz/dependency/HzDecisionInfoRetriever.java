package ru.taskurotta.backend.hz.dependency;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IQueue;
import ru.taskurotta.backend.console.model.GenericPage;
import ru.taskurotta.backend.console.retriever.DecisionInfoRetriever;
import ru.taskurotta.backend.hz.Constants;
import ru.taskurotta.transport.model.DecisionContainer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * DecisionInfoRetriever hazelcast aware implementation
 * User: dimadin
 * Date: 14.06.13 11:32
 *
 * For cooperative usage only with ru.taskurotta.backend.hz.server.HzTaskServer
 */
@Deprecated
public class HzDecisionInfoRetriever implements DecisionInfoRetriever {

    private HazelcastInstance hzInstance;

    private String queueListName = Constants.DEFAULT_QUEUE_LIST_NAME;

    public HzDecisionInfoRetriever(HazelcastInstance hzInstance) {
        this.hzInstance = hzInstance;
    }

    @Override
    public GenericPage<String> getQueueList(int pageNum, int pageSize) {
        List<String> result = new ArrayList<>();
        Set<String> queueNamesSet = filterDecisionQueues(hzInstance.<String>getSet(queueListName));
        String[] queueNames = queueNamesSet.toArray(new String[queueNamesSet.size()]);
        if (queueNames != null && queueNames.length > 0) {
            for (int i = (pageNum - 1) * pageSize; i <= ((pageSize * pageNum >= (queueNames.length)) ? (queueNames.length) - 1 : pageSize * pageNum - 1); i++) {
                result.add(queueNames[i]);
            }
        }
        return new GenericPage<String>(result, pageNum, pageSize, queueNames.length);
    }

    private Set<String> filterDecisionQueues(Set<String> queueNames) {
        Set<String> result = new HashSet<>();
        if(queueNames!=null && !queueNames.isEmpty()) {
            for(String queueName: queueNames) {
                if(queueName.contains(Constants.DECISION_QUEUE_PREFIX)) {
                    result.add(queueName);
                }
            }
        }
        return result;
    }


    @Override
    public int getQueueItemCount(String queueName) {
        return hzInstance.getQueue(queueName).size();
    }

    @Override
    public GenericPage<DecisionContainer> getQueueContent(String queueName, int pageNum, int pageSize) {
        List<DecisionContainer> result = new ArrayList<>();
        IQueue<DecisionContainer> queue = hzInstance.getQueue(queueName);
        DecisionContainer[] queueItems = queue.toArray(new DecisionContainer[queue.size()]);

        if (queueItems.length > 0) {
            for (int i = (pageNum - 1) * pageSize; i <= ((pageSize * pageNum >= (queueItems.length)) ? (queueItems.length) - 1 : pageSize * pageNum - 1); i++) {
                result.add(queueItems[i]);
            }
        }

        return new GenericPage<DecisionContainer>(result, pageNum, pageSize, queueItems.length);
    }

    public void setQueueListName(String queueListName) {
        this.queueListName = queueListName;
    }

}
