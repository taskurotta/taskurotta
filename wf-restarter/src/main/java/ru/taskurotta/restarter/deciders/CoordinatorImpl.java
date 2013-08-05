package ru.taskurotta.restarter.deciders;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.annotation.Asynchronous;
import ru.taskurotta.core.Promise;
import ru.taskurotta.restarter.workers.AnalyzerClient;
import ru.taskurotta.restarter.workers.RestarterClient;
import ru.taskurotta.transport.model.TaskContainer;

import java.util.Date;
import java.util.List;

/**
 * User: stukushin
 * Date: 01.08.13
 * Time: 17:51
 */
public class CoordinatorImpl implements Coordinator {

    private static Logger logger = LoggerFactory.getLogger(Coordinator.class);

    private AnalyzerClient analyzer;
    private RestarterClient restarter;
    private CoordinatorImpl asynchronous;

    private int taskBatchSize;

    @Override
    public void start() {
        logger.info("Start restart process coordinator at [{}]", new Date());

        asynchronous.findNotFinishedProcesses(analyzer.findNotFinishedProcesses(System.currentTimeMillis()));

        logger.info("Finish restart processes at [{}]", new Date());
    }

    @Asynchronous
    public void findNotFinishedProcesses(Promise<List<TaskContainer>> taskContainersPromise) {
        List<TaskContainer> taskContainers = taskContainersPromise.get();

        while (taskContainers != null && !taskContainers.isEmpty()) {
            Promise<Long> fromTimePromise = asynchronous.prepareForRestart(taskContainersPromise);

            asynchronous.findNotFinishedProcesses(analyzer.findNotFinishedProcesses(fromTimePromise.get()));
        }

        System.exit(0);
    }

    @Asynchronous
    public Promise<Long> prepareForRestart(Promise<List<TaskContainer>> promiseTaskContainers) {
        List<TaskContainer> taskContainers = promiseTaskContainers.get();

        logger.info("Start restarting tasks [{}]", taskContainers);

        long lastProcessStartTime = -1;
        while (taskContainers != null && !taskContainers.isEmpty()) {
            int size = taskBatchSize > taskContainers.size() ? taskBatchSize : taskContainers.size();

            List<TaskContainer> list = taskContainers.subList(0, size);
            logger.debug("Prepare [{}] task containers for restarting", list);

            restarter.restart(list);
            lastProcessStartTime = list.get(list.size() - 1).getStartTime();
            logger.debug("Send [{}] task containers to restarting", list.size());

            taskContainers.removeAll(list);
            logger.debug("Remove [{}] from task containers. Now [{}]", list.size(), taskContainers.size());
        }

        return Promise.asPromise(lastProcessStartTime);
    }

    public void setAnalyzer(AnalyzerClient analyzer) {
        this.analyzer = analyzer;
    }

    public void setRestarter(RestarterClient restarter) {
        this.restarter = restarter;
    }

    public void setAsynchronous(CoordinatorImpl asynchronous) {
        this.asynchronous = asynchronous;
    }

    public void setTaskBatchSize(int taskBatchSize) {
        this.taskBatchSize = taskBatchSize;
    }
}
