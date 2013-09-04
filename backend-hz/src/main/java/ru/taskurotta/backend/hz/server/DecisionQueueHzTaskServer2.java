package ru.taskurotta.backend.hz.server;

import java.io.Serializable;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ILock;
import com.hazelcast.core.IQueue;
import com.hazelcast.core.PartitionAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.backend.BackendBundle;
import ru.taskurotta.backend.config.ConfigBackend;
import ru.taskurotta.backend.dependency.DependencyBackend;
import ru.taskurotta.backend.queue.QueueBackend;
import ru.taskurotta.backend.storage.ProcessBackend;
import ru.taskurotta.backend.storage.TaskBackend;
import ru.taskurotta.transport.model.DecisionContainer;

/**
 * HazelcastTaskServer with distributed decisions queue, processed async with local ExecutorService threads.
 * Thread delegates processing to a distributed task
 * User: dimadin
 * Date: 15.07.13 12:20
 */
public class DecisionQueueHzTaskServer2 extends HazelcastTaskServer {

    private int decisionThreads = 1;
    private int pollTimeout = 60;
    private TimeUnit pollTimeoutTimeUnit = TimeUnit.SECONDS;

    private String decisionsQueueName = "defaultDecisionsProcessingQueue";

    private static final Logger logger = LoggerFactory.getLogger(DecisionQueueHzTaskServer.class);

    private long sleep = -1l;

    protected static DecisionQueueHzTaskServer2 instance;

    protected DecisionQueueHzTaskServer2(BackendBundle backendBundle) {
        super(backendBundle);
    }

    public DecisionQueueHzTaskServer2(ProcessBackend processBackend, TaskBackend taskBackend, QueueBackend queueBackend, DependencyBackend dependencyBackend, ConfigBackend configBackend) {
        super(processBackend, taskBackend, queueBackend, dependencyBackend, configBackend);
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
                                ExecutorService distributedExecutorService = hzInstance.getExecutorService(executorServiceName);
                                ProcessDecisionUnitOfWork call = new ProcessDecisionUnitOfWork(decisionToProcess.getProcessId(), decisionToProcess.getTaskId());
                                Future<Object> result = hzInstance.getExecutorService(executorServiceName).submit(call);
                                result.get();//Blocks thread until processing complete
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

    public static DecisionQueueHzTaskServer2 createInstance(BackendBundle backendBundle) {
        synchronized (instanceMonitor) {
            if (null == instance) {
                instance = new DecisionQueueHzTaskServer2(backendBundle);
                instanceMonitor.notifyAll();
            }
        }
        return instance;
    }

    public static DecisionQueueHzTaskServer2 createInstance(ProcessBackend processBackend, TaskBackend taskBackend, QueueBackend queueBackend, DependencyBackend dependencyBackend, ConfigBackend configBackend) {
        synchronized (instanceMonitor) {
            if (null == instance) {
                instance = new DecisionQueueHzTaskServer2(processBackend, taskBackend, queueBackend, dependencyBackend, configBackend);
                instanceMonitor.notifyAll();
            }
        }
        return instance;
    }

    //For obtaining reference to current TaskServer instance when processing async decision
    public static DecisionQueueHzTaskServer2 getInstance() throws InterruptedException {
        synchronized (instanceMonitor) {
            if (null == instance) {
                instanceMonitor.wait();
            }
        }
        return instance;
    }


    /**
     * Callable task for processing taskDecisions
     */
    public static class ProcessDecisionUnitOfWork implements Callable, PartitionAware, Serializable {
        private static AtomicInteger counter = new AtomicInteger(0);

        private static final Logger logger = LoggerFactory.getLogger(ProcessDecisionUnitOfWork.class);
        UUID processId;
        UUID taskId;
        String jobId = "undefined";

        public ProcessDecisionUnitOfWork() {
        }

        public ProcessDecisionUnitOfWork(UUID processId, UUID taskId) {
            this.processId = processId;
            this.taskId = taskId;

        }

        @Override
        public Object call() throws Exception {

            DecisionQueueHzTaskServer2 taskServer = DecisionQueueHzTaskServer2.getInstance();
            HazelcastInstance taskHzInstance = taskServer.getHzInstance();

            ILock lock = taskHzInstance.getLock(processId);
            try {
                lock.lock();
                DecisionContainer taskDecision = taskServer.getDecision(taskId, processId);
                if (taskDecision == null) {
                    String error = "Cannot get task decision from store by taskId[" + taskId + "], processId[" + processId + "]";
                    logger.error(error);
                    //TODO: this exception disappears for some reason
                    throw new IllegalStateException(error);
                }

                taskServer.processDecision(taskDecision);

            } finally {
                lock.unlock();
            }

            return null;
        }

        @Override
        public Object getPartitionKey() {
            return processId;
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
