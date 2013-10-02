package ru.taskurotta.schedule.manager;

import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.backend.console.retriever.QueueInfoRetriever;
import ru.taskurotta.schedule.JobVO;
import ru.taskurotta.server.TaskServer;
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
        JobVO job = (JobVO)jdm.get("job");
        TaskContainer taskContainer = job!=null? job.getTask(): null;
        TaskServer taskServer = (TaskServer)jdm.get("taskServer");
        QueueInfoRetriever queueInfoRetriever = (QueueInfoRetriever)jdm.get("queueInfoRetriever");

        try {

            validateEntities(taskServer, job, queueInfoRetriever);

            if (!job.isAllowDuplicates()) {
                int size = queueInfoRetriever.getQueueTaskCount(taskContainer.getActorId());
                if(size >0 ) {
                    logger.debug("Queue [{}] contains [{}] elements. Skip task - duplicates disallowed.", taskContainer.getActorId(), size);
                    return;
                }
            }

            taskContainer = renewTaskGuids(taskContainer);
            logger.debug("Starting process for task [{}] on schedule", taskContainer);
            taskServer.startProcess(taskContainer);

        } catch (Throwable e) {
            logger.error("Cannot execute scheduled job for task ["+taskContainer+"]", e);

            //TODO: dimadin: add job stop after repeated exceptions
        }
    }


    public void validateEntities(TaskServer taskServer, JobVO job, QueueInfoRetriever queueInfoRetriever) {
        if(taskServer == null) {
            throw new IllegalArgumentException("Scheduled job have no TaskServer job data entity!");
        }
        if(job == null) {
            throw new IllegalArgumentException("Scheduled job have no JobVO job data entity!");
        }
        if(job.getTask() == null) {
            throw new IllegalArgumentException("Scheduled job have no TaskContainer job data entity!");
        }
        if(!job.isAllowDuplicates() && queueInfoRetriever==null) {
            throw new IllegalArgumentException("Scheduled job have no QueueInfoRetriever job data entity!");
        }
    }

    public static TaskContainer renewTaskGuids(TaskContainer target) {
        UUID newGuid= UUID.randomUUID();
        return new TaskContainer(newGuid, newGuid, target.getMethod(), target.getActorId(), target.getType(), target.getStartTime(), target.getNumberOfAttempts(), target.getArgs(), target.getOptions());
    }


}
