package ru.taskurotta.service.hz.support;

import ru.taskurotta.service.console.model.GenericPage;
import ru.taskurotta.service.console.retriever.DecisionInfoRetriever;
import ru.taskurotta.service.storage.TaskService;
import ru.taskurotta.transport.model.DecisionContainer;

/**
 * Created with IntelliJ IDEA.
 * User: dimadin
 * Date: 01.07.13 15:44
 */
public class HzDecisionInfoRetriever implements DecisionInfoRetriever {

    private TaskService taskService;

    @Override
    public GenericPage<DecisionContainer> getActiveDecisions(int pageNum, int pageSize) {
        //TODO: implement it
        return null;
    }

}
