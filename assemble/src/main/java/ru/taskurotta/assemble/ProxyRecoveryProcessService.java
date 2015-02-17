package ru.taskurotta.assemble;

import ru.taskurotta.service.recovery.RecoveryProcessService;

import java.util.Collection;
import java.util.UUID;

/**
 * Date: 13.02.14 17:45
 */
public class ProxyRecoveryProcessService implements RecoveryProcessService {

    private RecoveryProcessService target;

    public ProxyRecoveryProcessService(RecoveryProcessService target) {
        this.target = target;
    }

    @Override
    public boolean resurrect(UUID processId) {
        return target.resurrect(processId);
    }

    @Override
    public Collection<UUID> restartProcessCollection(Collection<UUID> processIds) {
        return target.restartProcessCollection(processIds);
    }

}
