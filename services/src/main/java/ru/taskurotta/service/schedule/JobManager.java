package ru.taskurotta.service.schedule;

import ru.taskurotta.service.schedule.model.JobVO;

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
    boolean startJob(long id);

    /**
     * @return true if job execution was stopped successfully
     */
    boolean stopJob(long id);

    /**
     * @return currents state of the job: active, inactive etc
     */
    int getJobStatus(long id);

    /**
     * @return next execution time for the job
     */
    Date getNextExecutionTime(long id);

    /**
     * @return is job active
     */
    boolean isActive(JobVO job);

    /**
     * @return scheduled by this manager job ids (ie running local jobs collection)
     */
    Collection<Long> getScheduledJobIds();

    /**
     * Appends new Job to the store
     */
    long addJob(JobVO job);

    /**
     * Stops Job if running and removes it from the store
     */
    void removeJob(long id);

    /**
     * Provides list of all Job id's from the store (no matter what node it is)
     */
    Collection<Long> getJobIds();

    /**
     * @return JobVO object from the store
     */
    JobVO getJob(long id);

    /**
     * Updates status for the given Job at the store
     */
    void updateJobStatus(long id, int status);

    /**
     * Updates job entry at the store. Stops job if running.
     */
    void updateJob(JobVO jobVO);

    /**
     * Updates error counter for the job, appending last error message to the store
     */
    void updateErrorCount(long jobId, int count, String message);

    /**
     * Reads job statuses from the Store and starts the Job that should be running
     */
    void synchronizeScheduledTasksWithStore();

    Date getNextExecutionTime(String cron);


}
