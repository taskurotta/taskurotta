package ru.taskurotta.client.hazelcast.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.backend.hz.server.HazelcastTaskServer;
import ru.taskurotta.transport.model.DecisionContainer;

import java.io.Serializable;
import java.util.concurrent.Callable;

/**
 * User: stukushin
 * Date: 08.07.13
 * Time: 12:40
 */
public class ReleaseTask implements Callable<Void>, Serializable {

    private static final Logger logger = LoggerFactory.getLogger(ReleaseTask.class);

    private DecisionContainer decisionContainer;

    public ReleaseTask(DecisionContainer decisionContainer) {
        this.decisionContainer = decisionContainer;
    }

    @Override
    public Void call() throws Exception {
        logger.debug("Try to release decision container [{}]", decisionContainer);

        HazelcastTaskServer.getInstance().release(decisionContainer);

        logger.debug("Successfully release decision container [{}]", decisionContainer);

        return null;
    }
}