package ru.taskurotta.restarter.deciders;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.annotation.Asynchronous;
import ru.taskurotta.core.Promise;
import ru.taskurotta.restarter.ProcessVO;
import ru.taskurotta.restarter.workers.AnalyzerClient;
import ru.taskurotta.restarter.workers.RestarterClient;

import java.util.ArrayList;
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

        restartProcessesInternal(Promise.asPromise(System.currentTimeMillis()));
    }

    @Asynchronous
    public void restartProcesses(Promise<Long> fromTimePromise) {

        if (fromTimePromise.get() == -1) {
            if (logger.isInfoEnabled()) {
                logger.info("Finish restart process coordinator at [{}]", new Date());
            }

            return;
        }

        restartProcessesInternal(fromTimePromise);
    }

    private void restartProcessesInternal(Promise<Long> fromTimePromise) {
        Promise<List<ProcessVO>> processesPromise = analyzer.findNotFinishedProcesses(fromTimePromise.get());

        fromTimePromise = asynchronous.prepareForRestart(processesPromise);

        asynchronous.restartProcesses(fromTimePromise);
    }

    @Asynchronous
    public Promise<Long> prepareForRestart(Promise<List<ProcessVO>> processesPromise) {
        List<ProcessVO> processes = processesPromise.get();

        if (processes == null || processes.isEmpty()) {
            return Promise.asPromise(-1l);
        }

        if (logger.isInfoEnabled()) {
            logger.info("Start restarting [{}] processes", processes.size());
        }

        int processesSize = processes.size();
        int batchesCount = taskBatchSize < processesSize ? processesSize / taskBatchSize : 1;

        for (int fromIndex = 0; fromIndex < batchesCount; fromIndex++) {
            int toIndex = (fromIndex + 1) * taskBatchSize < processesSize ? (fromIndex + 1) * taskBatchSize : processesSize - fromIndex * taskBatchSize;

            List<ProcessVO> list = new ArrayList<>(processes.subList(fromIndex, toIndex));
            if (logger.isDebugEnabled()) {
                logger.debug("Prepare [{}] processes for restarting", list.size());
            }

            restarter.restart(list);
            if (logger.isDebugEnabled()) {
                logger.debug("Send [{}] processes to restarting", list.size());
            }
        }

        long earlyProcessStartTime = processes.get(0).getStartTime();
        if (logger.isDebugEnabled()) {
            logger.debug("Early restart process start time is [{}]({})", earlyProcessStartTime, new Date(earlyProcessStartTime));
        }

        return Promise.asPromise(earlyProcessStartTime);
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
