package ru.taskurotta.test.stress;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ILock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.service.console.model.InterruptedTask;
import ru.taskurotta.service.executor.OperationExecutor;
import ru.taskurotta.service.recovery.RecoveryService;
import ru.taskurotta.service.recovery.RestartTaskOperation;
import ru.taskurotta.service.storage.InterruptedTasksService;
import ru.taskurotta.util.DaemonThread;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 */
public class AutoResurrectService {

    private static final Logger logger = LoggerFactory.getLogger(AutoResurrectService.class);

    public AutoResurrectService(final InterruptedTasksService interruptedTasksService, final
    OperationExecutor<RecoveryService> operationExecutor, HazelcastInstance hazelcastInstance) {


        new DaemonThread("process resurrection thread", TimeUnit.SECONDS, 1) {

            @Override
            public void daemonJob() {

                final ILock lock = hazelcastInstance.getLock(AutoResurrectService.class.getName());

                lock.lock();

                try {
                    Collection<InterruptedTask> allInterruptedTasks = interruptedTasksService.findAll();
                    if (allInterruptedTasks != null) {
                        logger.debug("Found [{}] interrupted tasks", allInterruptedTasks.size());

                        for (InterruptedTask itdTask : allInterruptedTasks) {

                            if (itdTask.getErrorClassName().equals(BrokenProcessException.class.getName())) {
                                operationExecutor.enqueue(new RestartTaskOperation(itdTask.getProcessId(), itdTask.getTaskId()));
                            }
                        }

                    }

                } finally {
                    lock.unlock();
                }
            }
        }.start();
    }
}
