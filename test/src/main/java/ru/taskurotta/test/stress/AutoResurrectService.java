package ru.taskurotta.test.stress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.service.console.model.BrokenProcess;
import ru.taskurotta.service.executor.OperationExecutor;
import ru.taskurotta.service.recovery.RecoveryOperation;
import ru.taskurotta.service.storage.BrokenProcessService;
import ru.taskurotta.util.DaemonThread;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 */
public class AutoResurrectService {

    private static final Logger logger = LoggerFactory.getLogger(AutoResurrectService.class);

    public AutoResurrectService(final BrokenProcessService brokenProcessService, final OperationExecutor operationExecutor) {

        new DaemonThread("process resurrection thread", TimeUnit.SECONDS, 15) {

            @Override
            public void daemonJob() {
                Collection<BrokenProcess> allBrokenProcesses = brokenProcessService.findAll();

                logger.debug("Find {} broken processes", allBrokenProcesses.size());

                for (BrokenProcess brokenProcess: allBrokenProcesses) {

                    if (brokenProcess.getErrorClassName().equals(BrokenProcessException.class.getName())) {
                        operationExecutor.enqueue(new RecoveryOperation(brokenProcess.getProcessId()));
                    }
                }
            }
        }.start();

    }
}
