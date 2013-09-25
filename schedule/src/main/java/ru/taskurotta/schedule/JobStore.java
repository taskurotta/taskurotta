package ru.taskurotta.schedule;

import java.util.Collection;

/**
 * User: dimadin
 * Date: 23.09.13 10:31
 */
public interface JobStore {

    public long addJob(JobVO task);

    public void removeJob(long id);

    public Collection<Long> getJobIds();

    public JobVO getJob(long id);

    public void updateJobStatus(long id, int status);

}
