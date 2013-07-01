package ru.taskurotta.backend.console.retriever;

import ru.taskurotta.backend.console.model.GenericPage;
import ru.taskurotta.transport.model.DecisionContainer;

/**
 * Created with IntelliJ IDEA.
 * User: dimadin
 * Date: 14.06.13 11:20
 */
public interface DecisionInfoRetriever {

    public GenericPage<String> getQueueList(int pageNum, int pageSize);

    public int getQueueItemCount(String queueName);

    public GenericPage<DecisionContainer> getQueueContent(String queueName, int pageNum, int pageSize);

}
