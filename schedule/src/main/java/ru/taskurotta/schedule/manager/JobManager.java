package ru.taskurotta.schedule.manager;

import ru.taskurotta.schedule.JobVO;

import java.util.Collection;
import java.util.Date;

/**
 * User: dimadin
 * Date: 23.09.13 19:12
 */
public interface JobManager {

    /**
     * @return true if job execution was started successfully
     */
    public boolean startJob(long id);

    /**
     * @return true if job execution was stopped successfully
     */
    public boolean stopJob(long id);

    /**
     * @return currents state of the job: active, inactive etc
     */
    public int getJobStatus(long id);

    /**
     * @return next execution time for the job
     */
    public Date getNextExecutionTime(long id);

    /**
     * @return is job active
     */
    public boolean isActive(JobVO job);

    /**
     * @return scheduled by this manager job ids (ie running local jobs collection)
     */
    public Collection<Long> getScheduledJobIds();

    /**
     * Appends new Job to the store
     * @param job
     * @return
     */
    public long addJob(JobVO job);

    /**
     * Stops Job if running and removes it from the store
     * @param id
     */
    public void removeJob(long id);

    /**
     * Provides list of all Job id's from the store (no matter what node it is)
     * @return
     */
    public Collection<Long> getJobIds();

    /**
     * @return JobVO object from the store
     */
    public JobVO getJob(long id);

    /**
     * Updates status for the given Job at the store
     */
    public void updateJobStatus(long id, int status);

    /**
     * Updates job entry at the store. Stops job if running.
     */
    public void updateJob(JobVO jobVO);

    /**
     * Updates error counter for the job, appending last error message to the store
     */
    public void updateErrorCount(long jobId, int count, String message);

    /**
     * Reads job statuses from the Store and starts the Job that should be running
     */
    public void synchronizeScheduledTasksWithStore();

}
