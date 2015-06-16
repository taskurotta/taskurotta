package ru.taskurotta.service.schedule.storage;

import ru.taskurotta.service.schedule.JobConstants;
import ru.taskurotta.service.schedule.model.JobVO;
import ru.taskurotta.service.storage.impl.JsonEntityStore;

/**
 * Implementation of a job store using files in JSON format to store jobs.
 * It is strongly recommended to use this implementation only for development/testing purposes
 *
 * Date: 09.12.13 13:35
 */
public class SchedulerJsonJobStore extends JsonEntityStore<JobVO> implements JobStore {

    public SchedulerJsonJobStore(String storeLocation) {
        super(JobVO.class, storeLocation);
    }

    @Override
    public long add(JobVO task) {
        long result = super.add(task);
        if (result>0) {
            task.setId(result);
        }
        return result;
    }

    @Override
    public JobVO get(long id) {
        JobVO result = super.get(id);
        if (result != null) {
            result.setId(id);
        }
        return result;
    }

    @Override
    public void updateJobStatus(long id, int status) {
        JobVO jobVO = get(id);
        if (jobVO != null) {
            jobVO.setId(id);
            jobVO.setStatus(status);
            update(jobVO, id);
        }
    }

    @Override
    public int getJobStatus(long jobId) {
        int result = JobConstants.STATUS_UNDEFINED;
        JobVO job = get(jobId);
        if (job!=null) {
            result = job.getStatus();
        }
        return result;
    }

    @Override
    public void updateErrorCount(long jobId, int count, String message) {
        JobVO job = get(jobId);
        if (job!=null) {
            job.setId(jobId);
            job.setErrorCount(count);
            job.setLastError(message);
            update(job, jobId);
        }
    }

}
