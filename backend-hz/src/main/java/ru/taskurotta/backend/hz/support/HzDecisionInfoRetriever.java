package ru.taskurotta.backend.hz.support;

import ru.taskurotta.backend.console.model.GenericPage;
import ru.taskurotta.backend.console.retriever.DecisionInfoRetriever;
import ru.taskurotta.backend.storage.TaskBackend;
import ru.taskurotta.transport.model.DecisionContainer;

/**
 * Created with IntelliJ IDEA.
 * User: dimadin
 * Date: 01.07.13 15:44
 */
public class HzDecisionInfoRetriever implements DecisionInfoRetriever {

    private TaskBackend taskBackend;

    @Override
    public GenericPage<DecisionContainer> getActiveDecisions(int pageNum, int pageSize) {
        //TODO: implement it
        return null;
    }

}
