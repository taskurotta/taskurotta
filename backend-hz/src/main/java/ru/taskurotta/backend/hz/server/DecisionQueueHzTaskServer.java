package ru.taskurotta.backend.hz.server;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.backend.BackendBundle;
import ru.taskurotta.backend.config.ConfigBackend;
import ru.taskurotta.backend.dependency.DependencyBackend;
import ru.taskurotta.backend.process.BrokenProcessBackend;
import ru.taskurotta.backend.queue.QueueBackend;
import ru.taskurotta.backend.storage.ProcessBackend;
import ru.taskurotta.backend.storage.TaskBackend;
import ru.taskurotta.transport.model.DecisionContainer;

/**
 * HazelcastTaskServer with distributed decisions queue, processed async with local ExecutorService threads
 * User: dimadin
 * Date: 15.07.13 10:23
 */
public class DecisionQueueHzTaskServer extends HazelcastTaskServer {

    private int decisionThreads = 1;
    private int pollTimeout = 60;
    private TimeUnit pollTimeoutTimeUnit = TimeUnit.SECONDS;

    private String decisionsQueueName = "defaultDecisionsProcessingQueue";

    private static final Logger logger = LoggerFactory.getLogger(DecisionQueueHzTaskServer.class);

    private long sleep = -1l;

    protected DecisionQueueHzTaskServer(BackendBundle backendBundle) {
        super(backendBundle);
    }

    public DecisionQueueHzTaskServer(ProcessBackend processBackend, TaskBackend taskBackend, QueueBackend queueBackend, DependencyBackend dependencyBackend, ConfigBackend configBackend, BrokenProcessBackend brokenProcessBackend) {
        super(processBackend, taskBackend, queueBackend, dependencyBackend, configBackend, brokenProcessBackend);
    }

    public void setDecisionThreads(int decisionThreads) {
        this.decisionThreads = decisionThreads;
    }

    public void initiateDecisionProcessing() {
        ExecutorService executorService = Executors.newFixedThreadPool(decisionThreads, new ThreadFactory() {
            private int counter = 1;

            @Override
            public Thread newThread(Runnable r) {
                Thread result = new Thread(r);
                result.setDaemon(true);
                result.setName("Decision processor - " + counter++);
                return result;
            }
        });

        for (int i = 0; i < decisionThreads; i++) {
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    while (true) {
                        DecisionContainer decisionToProcess = null;
                        try {
                            HazelcastInstance hzInstance = getHzInstance();
                            IQueue<DecisionContainer> queue = hzInstance.getQueue(decisionsQueueName);
                            decisionToProcess = queue.poll(pollTimeout, pollTimeoutTimeUnit);

                            if (decisionToProcess != null) {
                                if (sleep > 0) {
                                    Thread.sleep(sleep);
                                }
                                processDecision(decisionToProcess);
                            }
                        } catch (Throwable e) {
                            logger.error("Error processing decision [" + decisionToProcess + "]", e);
                            continue;
                        }
                    }
                }
            });
        }
    }


    @Override
    public void release(DecisionContainer taskDecision) {
        logger.debug("DQHZ server release for decision [{}]", taskDecision);

        // save it in task backend
        taskBackend.addDecision(taskDecision);

        //Enqueue decision for processing
        IQueue<DecisionContainer> queue = getHzInstance().getQueue(decisionsQueueName);
        queue.add(taskDecision);
    }

    public void setDecisionsQueueName(String decisionsQueueName) {
        this.decisionsQueueName = decisionsQueueName;
    }

    public void setPollTimeout(int pollTimeout) {
        this.pollTimeout = pollTimeout;
    }

    public void setPollTimeoutTimeUnit(TimeUnit pollTimeoutTimeUnit) {
        this.pollTimeoutTimeUnit = pollTimeoutTimeUnit;
    }

    public void setSleep(long sleep) {
        this.sleep = sleep;
    }
}
