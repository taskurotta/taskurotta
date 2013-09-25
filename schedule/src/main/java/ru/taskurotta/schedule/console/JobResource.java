package ru.taskurotta.schedule.console;

import com.google.common.base.Optional;
import org.quartz.CronExpression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;
import ru.taskurotta.schedule.JobConstants;
import ru.taskurotta.schedule.JobVO;
import ru.taskurotta.schedule.adapter.HzJobMessageHandler;
import ru.taskurotta.schedule.adapter.HzMessage;
import ru.taskurotta.schedule.storage.JobStore;
import ru.taskurotta.transport.model.TaskContainer;
import ru.taskurotta.transport.model.TaskType;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.Serializable;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Rest resource providing data on schedulers for console
 * User: dimadin
 * Date: 23.09.13 14:58
 */
@Path("/console/schedule/{action}")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class JobResource implements JobConstants {

    private static final Logger logger = LoggerFactory.getLogger(JobResource.class);
    private JobStore jobStore;
    private HzJobMessageHandler hzScheduleEventDispatcher;

    @GET
    public Response processGet(@PathParam("action")String action) {
        if (ACTION_LIST.equals(action)) {
            Collection<Long> taskIds = jobStore.getJobIds();
            List<JobExtVO> result = null;
            if (taskIds!=null && !taskIds.isEmpty()) {
                result = new ArrayList<>();
                for (Long id: taskIds) {
                    JobVO task = jobStore.getJob(id);
                    if (task != null) {
                        JobExtVO taskExt = new JobExtVO(task);
                        taskExt.nextExecutionTime = getNextExecutionTime(task.getCron());
                        result.add(taskExt);
                    }
                }
            }
            return Response.ok(result, MediaType.APPLICATION_JSON).build();
        } else {
            logger.error("Unsupported combination of method[GET] and action["+action+"].");
            return Response.serverError().build();
        }
    }

    public static class JobExtVO extends JobVO implements Serializable {
        protected Date nextExecutionTime;

        public JobExtVO(JobVO job) {
            this.id = job.getId();
            this.name = job.getName();
            this.cron = job.getCron();
            this.task = job.getTask();
            this.status = job.getStatus();
        }

        public Date getNextExecutionTime() {
            return nextExecutionTime;
        }
    }

    @PUT
    public Response addScheduledTask(@PathParam("action")String action, @QueryParam("cron")Optional<String> cronOpt, @QueryParam("name")Optional<String> nameOpt, @QueryParam("allowDuplicates")Optional<Boolean> duplicatesOtp, TaskContainer task) {
        String cron = cronOpt.or("");
        String name = nameOpt.or("");
        Boolean allowDuplicates = duplicatesOtp.or(Boolean.TRUE);
        if (ACTION_CREATE.equals(action)) {
            logger.debug("Creating scheduled task for cron [{}], name[{}] and TaskContainer[{}]", cronOpt.or(""), nameOpt.or(""), task);

            JobVO job = new JobVO();
            job.setCron(cron);
            job.setAllowDuplicates(allowDuplicates);
            job.setName(name);
            job.setTask(extendTask(task));
            job.setStatus(STATUS_INACTIVE);

            long id = jobStore.addJob(job);
            logger.debug("Scheduled task for name[{}], cron[{}] added with id[{}]", name, cron, id);
            return Response.ok(id, MediaType.APPLICATION_JSON).build();
        } else {
            logger.error("Unsupported combination of method[POST] and action[" + action + "].");
            return Response.serverError().build();
        }

    }

    @POST
    public Response processTask(@PathParam("action")String action, @QueryParam("id") Long id) {

        if (isActionSupported(action)) {
            logger.debug("Sending schedule message for id [{}]", id);
            hzScheduleEventDispatcher.dispatch(new HzMessage(id, action));
            return Response.ok().build();

        } else {
            logger.error("Unsupported combination of method[POST] and action[" + action + "].");
            return Response.serverError().build();
        }

    }

    private boolean isActionSupported(String action) {
        return ACTION_DELETE.equals(action) || ACTION_ACTIVATE.equals(action) || ACTION_DEACTIVATE.equals(action);
    }

    public static TaskContainer extendTask(TaskContainer target) {
        UUID taskId = target.getTaskId()!=null? target.getTaskId(): UUID.randomUUID();
        UUID processId = target.getProcessId()!=null? target.getProcessId(): UUID.randomUUID();
        TaskType type = target.getType()!=null? target.getType(): TaskType.DECIDER_START;
        long startTime = target.getStartTime()!=0? target.getStartTime(): -1;
        int numberOfAttempts = target.getNumberOfAttempts()!=0? target.getNumberOfAttempts(): 5;

        return new TaskContainer(taskId, processId, target.getMethod(), target.getActorId(), type, startTime, numberOfAttempts, target.getArgs(), target.getOptions());
    }


    public Date getNextExecutionTime(String cron) {
        Date result = null;

        try {
            result = new CronExpression(cron).getNextValidTimeAfter(new Date());
        } catch (ParseException e) {
            logger.error("Cannot parse cron " + cron, e);
        }

        return result;
    }

    @Required
    public void setJobStore(JobStore jobStore) {
        this.jobStore = jobStore;
    }

    @Required
    public void setHzScheduleEventDispatcher(HzJobMessageHandler hzScheduleEventDispatcher) {
        this.hzScheduleEventDispatcher = hzScheduleEventDispatcher;
    }

}
