package ru.taskurotta.service.recovery;

import ru.taskurotta.service.metrics.Metric;
import ru.taskurotta.service.metrics.MetricName;
import ru.taskurotta.service.metrics.MetricsFactory;

import java.util.Collection;
import java.util.UUID;

/**
 * Wrapper for RecoveryProcessService providing metrics data gathering for console
 * Date: 13.02.14 12:46
 */
public class MetricsRecoveryService implements RecoveryService {

    private RecoveryService recoveryService;
    private MetricsFactory metricsFactory;

    public MetricsRecoveryService(RecoveryService recoveryService, MetricsFactory metricsFactory) {
        this.recoveryService = recoveryService;
        this.metricsFactory = metricsFactory;
    }

    @Override
    public boolean resurrectProcess(UUID processId) {
        long start = System.currentTimeMillis();
        boolean result = recoveryService.resurrectProcess(processId);
        long period = System.currentTimeMillis() - start;
        Metric recoveryMetric = metricsFactory.getInstance(MetricName.RECOVERY.getValue());
        recoveryMetric.mark(MetricName.RECOVERY.getValue(), period);

        if (result) {
            recoveryMetric.mark("restartSuccess", period);
        } else {
            recoveryMetric.mark("restartSkip", period);
        }

        return result;
    }

    @Override
    public Collection<UUID> resurrectProcesses(Collection<UUID> processIds) {
        return recoveryService.resurrectProcesses(processIds);
    }

    @Override
    public boolean abortProcess(UUID processId) {
        return recoveryService.abortProcess(processId);
    }

    @Override
    public boolean restartTask(UUID processId, UUID taskId) {
        return recoveryService.restartTask(processId, taskId);
    }

    @Override
    public boolean resurrectTask(UUID taskId, UUID processId) {
        return recoveryService.resurrectTask(taskId, processId);
    }
}
