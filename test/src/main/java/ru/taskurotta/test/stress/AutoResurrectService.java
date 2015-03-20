package ru.taskurotta.test.stress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.service.console.model.InterruptedTask;
import ru.taskurotta.service.executor.OperationExecutor;
import ru.taskurotta.service.recovery.RecoveryOperation;
import ru.taskurotta.service.storage.InterruptedTasksService;
import ru.taskurotta.util.DaemonThread;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 */
public class AutoResurrectService {

    private static final Logger logger = LoggerFactory.getLogger(AutoResurrectService.class);

    public AutoResurrectService(final InterruptedTasksService interruptedTasksService, final OperationExecutor operationExecutor) {

        new DaemonThread("process resurrection thread", TimeUnit.SECONDS, 1) {

            @Override
            public void daemonJob() {
                Collection<InterruptedTask> allInterruptedTasks = interruptedTasksService.findAll();

                logger.debug("Found [{}] interrupted tasks", allInterruptedTasks.size());

                for (InterruptedTask itdTask: allInterruptedTasks) {

                    if (itdTask.getErrorClassName().equals(BrokenProcessException.class.getName())) {
                        operationExecutor.enqueue(new RecoveryOperation(itdTask.getProcessId()));
                    }
                }
            }
        }.start();

    }
}
