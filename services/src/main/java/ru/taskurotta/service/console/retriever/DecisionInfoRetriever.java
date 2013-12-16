package ru.taskurotta.service.console.retriever;

import ru.taskurotta.service.console.model.GenericPage;
import ru.taskurotta.transport.model.DecisionContainer;

/**
 * Retrieves information on task server's actor decisions processing
 * User: dimadin
 * Date: 14.06.13 11:20
 */
public interface DecisionInfoRetriever {

    /**
     * @return paginated list of decisions awaiting processing
     */
    public GenericPage<DecisionContainer> getActiveDecisions(int pageNum, int pageSize);

}
