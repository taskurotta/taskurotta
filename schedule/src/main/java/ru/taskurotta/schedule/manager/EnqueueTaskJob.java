package ru.taskurotta.schedule.manager;

import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.backend.hz.server.HazelcastTaskServer;
import ru.taskurotta.transport.model.TaskContainer;

import java.util.UUID;

/**
 * User: dimadin
 * Date: 24.09.13 12:40
 */
public class EnqueueTaskJob implements Job {

    private static final Logger logger = LoggerFactory.getLogger(EnqueueTaskJob.class);

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        JobDataMap jdm = context.getJobDetail().getJobDataMap();
        TaskContainer taskContainer = (TaskContainer)jdm.get("task");
        try {

            HazelcastTaskServer hzTaskServer = HazelcastTaskServer.getInstance();
            if (taskContainer != null) {
                taskContainer = renewTaskGuids(taskContainer);
                logger.debug("Starting process for task [{}] on schedule", taskContainer);
                hzTaskServer.startProcess(taskContainer);

            } else {
                throw new IllegalArgumentException("Scheduled job have no TaskContainer entity!");
            }

        } catch (Throwable e) {
            logger.error("Cannot execute scheduled job for task ["+taskContainer+"]", e);
        }
    }


    public static TaskContainer renewTaskGuids(TaskContainer target) {
        UUID newGuid= UUID.randomUUID();
        return new TaskContainer(newGuid, newGuid, target.getMethod(), target.getActorId(), target.getType(), target.getStartTime(), target.getNumberOfAttempts(), target.getArgs(), target.getOptions());
    }


}
