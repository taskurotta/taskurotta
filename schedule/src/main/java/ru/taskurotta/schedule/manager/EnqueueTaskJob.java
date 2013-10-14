package ru.taskurotta.schedule.manager;

import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.backend.console.retriever.QueueInfoRetriever;
import ru.taskurotta.schedule.JobConstants;
import ru.taskurotta.schedule.JobVO;
import ru.taskurotta.schedule.storage.JobStore;
import ru.taskurotta.server.TaskServer;
import ru.taskurotta.transport.model.TaskContainer;

import java.util.UUID;

/**
 * User: dimadin
 * Date: 24.09.13 12:40
 */
public class EnqueueTaskJob implements Job {

    private static final Logger logger = LoggerFactory.getLogger(EnqueueTaskJob.class);

    public static final int MAX_CONSEQUENTIAL_ERRORS = 3;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        JobDataMap jdm = context.getJobDetail().getJobDataMap();
        JobVO job = (JobVO)jdm.get("job");
        TaskContainer taskContainer = job!=null? job.getTask(): null;
        TaskServer taskServer = (TaskServer)jdm.get("taskServer");
        JobStore jobStore = (JobStore)jdm.get("jobStore");
        QueueInfoRetriever queueInfoRetriever = (QueueInfoRetriever)jdm.get("queueInfoRetriever");

        try {

            validateEntities(taskServer, job, queueInfoRetriever, jobStore);

            if (job.getQueueLimit()>0) {
                int size = queueInfoRetriever.getQueueTaskCount(taskContainer.getActorId());
                if (size >= job.getQueueLimit() ) {
                    logger.debug("Queue [{}] contains [{}] elements. Skip task due to limit[{}].", taskContainer.getActorId(), size, job.getQueueLimit());
                    return;
                }
            }

            taskContainer = renewTaskGuids(taskContainer);
            logger.debug("Starting process for task [{}] on schedule", taskContainer);

            taskServer.startProcess(taskContainer);

            if (job.getErrorCount()>0) {
                job.setErrorCount(0);
                job.setLastError("");
                jobStore.updateErrorCount(job.getId(), job.getErrorCount(), job.getLastError());//reset error counter
            }

        } catch (Throwable e) {
            logger.error("Cannot execute scheduled job for task ["+taskContainer+"]", e);

            if (jobStore != null && job!=null && job.getId()>0) {
                job.setErrorCount(job.getErrorCount()+1);
                job.setLastError(e.getClass().getName() + ": " + e.getMessage());
                try {
                    jobStore.updateErrorCount(job.getId(), job.getErrorCount(), job.getLastError());

                    if (job.getErrorCount()+1>=MAX_CONSEQUENTIAL_ERRORS) {
                        JobManager jobManager = (JobManager) jdm.get("jobManager");
                        if (jobManager != null && jobManager.stopJob(job.getId())) {
                            jobStore.updateJobStatus(job.getId(), JobConstants.STATUS_ERROR);
                        }
                    }

                } catch(Throwable err) {
                    logger.error("Error at error handling for job ["+job+"]", e);
                }

            }
        }
    }


    public void validateEntities(TaskServer taskServer, JobVO job, QueueInfoRetriever queueInfoRetriever, JobStore jobStore) {
        if(taskServer == null) {
            throw new IllegalArgumentException("Scheduled job have no TaskServer job data entity!");
        }
        if(job == null) {
            throw new IllegalArgumentException("Scheduled job have no JobVO job data entity!");
        }
        if(job.getTask() == null) {
            throw new IllegalArgumentException("Scheduled job have no TaskContainer job data entity!");
        }
        if(job.getQueueLimit()>0 && queueInfoRetriever==null) {
            throw new IllegalArgumentException("Scheduled job have no QueueInfoRetriever job data entity!");
        }
        if (jobStore == null) {
            throw new IllegalArgumentException("Scheduled job have no JobStore job data entity!");
        }
    }

    public static TaskContainer renewTaskGuids(TaskContainer target) {
        UUID newGuid= UUID.randomUUID();
        return new TaskContainer(newGuid, newGuid, target.getMethod(), target.getActorId(), target.getType(), target.getStartTime(), target.getNumberOfAttempts(), target.getArgs(), target.getOptions());
    }


}
