package ru.taskurotta.schedule.manager;

import ru.taskurotta.schedule.JobVO;

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

}
