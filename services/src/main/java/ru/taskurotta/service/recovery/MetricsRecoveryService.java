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
public class MetricsRecoveryService implements RecoveryProcessService {

    private RecoveryProcessService recoveryProcessService;
    private MetricsFactory metricsFactory;

    public MetricsRecoveryService(RecoveryProcessService recoveryProcessService, MetricsFactory metricsFactory) {
        this.recoveryProcessService = recoveryProcessService;
        this.metricsFactory = metricsFactory;
    }

    @Override
    public boolean resurrect(UUID processId) {
        long start = System.currentTimeMillis();
        boolean result = recoveryProcessService.resurrect(processId);
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
    public Collection<UUID> restartProcessCollection(Collection<UUID> processIds) {
        return recoveryProcessService.restartProcessCollection(processIds);
    }
}
