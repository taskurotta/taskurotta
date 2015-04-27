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
    public boolean recoverProcess(UUID processId) {
        return target.recoverProcess(processId);
    }

    @Override
    public Collection<UUID> recoverProcesses(Collection<UUID> processIds) {
        return target.recoverProcesses(processIds);
    }

    @Override
    public boolean recoverTask(UUID processId, UUID taskId) {
        return target.recoverTask(processId, taskId);
    }

}
