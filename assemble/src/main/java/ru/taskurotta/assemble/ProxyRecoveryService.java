package ru.taskurotta.assemble;

import ru.taskurotta.service.recovery.RecoveryService;

import java.util.Collection;
import java.util.UUID;

/**
 * Date: 13.02.14 17:45
 */
public class ProxyRecoveryService implements RecoveryService {

    private RecoveryService target;

    public ProxyRecoveryService(RecoveryService target) {
        this.target = target;
    }

    @Override
    public boolean resurrectProcess(UUID processId) {
        return target.resurrectProcess(processId);
    }

    @Override
    public Collection<UUID> resurrectProcesses(Collection<UUID> processIds) {
        return target.resurrectProcesses(processIds);
    }

    @Override
    public boolean abortProcess(UUID processId) {
        return target.abortProcess(processId);
    }

    @Override
    public boolean restartInterruptedTask(UUID processId, UUID taskId) {
        return target.restartInterruptedTask(processId, taskId);
    }

    @Override
    public boolean reenqueueTask(UUID taskId, UUID processId) {
        return target.reenqueueTask(taskId, processId);
    }
}
