package ru.taskurotta.service.hz.schedule.adapter;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IExecutorService;
import com.hazelcast.core.Member;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.service.schedule.model.JobVO;
import ru.taskurotta.service.hz.schedule.adapter.tasks.AddJobCallable;
import ru.taskurotta.service.hz.schedule.adapter.tasks.RemoveJobRunnable;
import ru.taskurotta.service.hz.schedule.adapter.tasks.UpdateErrorCountRunnable;
import ru.taskurotta.service.hz.schedule.adapter.tasks.UpdateJobRunnable;
import ru.taskurotta.service.hz.schedule.adapter.tasks.UpdateJobStatusRunnable;
import ru.taskurotta.service.schedule.storage.JobStore;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.Future;

/**
 * If job store is not shared, distribute JobStore's method calls to be executed on every
 * hazelcast node (via distributed executor service)
 *
 * In case of shared store, just delegates calls to the real store implementation
 *
 * Date: 10.12.13 14:35
 */
public class HzJobStoreAdapter implements JobStore {
    private static final Logger logger = LoggerFactory.getLogger(HzJobStoreAdapter.class);
    private IExecutorService executorService;
    private static JobStore jobStore;

    private boolean isSharedStore = false;

    public HzJobStoreAdapter(JobStore jobStore, HazelcastInstance hzInstance, boolean isSharedStore) {
        this.jobStore = jobStore;
        this.executorService = hzInstance.getExecutorService(getClass().getName());
        this.isSharedStore = isSharedStore;
        logger.debug("Using hazelcast cluster adapter for {} jobStore", (isSharedStore? "shared": "separate"));
    }

    public static JobStore getRealJobStore() {
        return jobStore;
    }

    @Override
    public long add(final JobVO job) {
        long result = -1l;

        if (isSharedStore) {
            result = jobStore.add(job);
        } else {
            Map<Member, Future<Long>> nodesResults = executorService.submitToAllMembers(new AddJobCallable(job));

            for (Future<Long> nodeResultFuture: nodesResults.values()) {
                Long nodeResult = null;
                try {
                    nodeResult = nodeResultFuture.get();
                    if (nodeResult != null) {
                        long newResult = nodeResult.longValue();
                        if (result < 0 || result == newResult) {//new result or the same as on prev node: case OK
                            result = newResult;

                        } else {//different results from nodes: nodes unsync, error state
                            throw new IllegalStateException("Cannot execute addJob["+job+"], nodes are not synchronized!");
                        }
                    } else {
                        throw new IllegalStateException("Cannot execute addJob["+job+"], node result is null!");
                    }
                } catch (Exception e) {
                    logger.error("addJob[" + job + "] execution interrupted, possible nodes desynchronization", e);
                    result =  -1l;
                    break;
                }
            }
        }

        return result;
    }

    @Override
    public void remove(long id) {
        if (isSharedStore) {
            jobStore.remove(id);
        } else {
            executorService.executeOnAllMembers(new RemoveJobRunnable(id));
        }
    }

    @Override
    public void updateJobStatus(long id, int status) {
        executorService.executeOnAllMembers(new UpdateJobStatusRunnable(id, status));
    }

    @Override
    public void update(final JobVO jobVO, long id) {
        if (isSharedStore) {
            jobStore.update(jobVO, id);
        } else {
            executorService.executeOnAllMembers(new UpdateJobRunnable(jobVO));
        }
    }

    @Override
    public void updateErrorCount(final long jobId, final int count, final String message) {
        if (isSharedStore) {
            jobStore.updateErrorCount(jobId, count, message);
        } else {
            executorService.executeOnAllMembers(new UpdateErrorCountRunnable(jobId, count, message));
        }
    }

    @Override
    public Collection<Long> getKeys() {
        return jobStore.getKeys();
    }

    @Override
    public JobVO get(long id) {
        return jobStore.get(id);
    }

    @Override
    public int getJobStatus(long jobId) {
        return jobStore.getJobStatus(jobId);
    }

}
