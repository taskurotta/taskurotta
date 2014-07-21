package ru.taskurotta.dropwizard.resources.console.schedule;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import org.quartz.CronExpression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.util.StringUtils;
import ru.taskurotta.dropwizard.resources.console.schedule.model.ArgVO;
import ru.taskurotta.dropwizard.resources.console.schedule.model.CreateJobCommand;
import ru.taskurotta.internal.core.TaskType;
import ru.taskurotta.service.console.Action;
import ru.taskurotta.service.schedule.JobConstants;
import ru.taskurotta.service.schedule.JobManager;
import ru.taskurotta.service.schedule.model.JobVO;
import ru.taskurotta.transport.model.ArgContainer;
import ru.taskurotta.transport.model.TaskContainer;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;
import java.io.Serializable;
import java.text.ParseException;
import java.util.*;

/**
 * Rest resource providing data on schedulers for console
 * User: dimadin
 * Date: 23.09.13 14:58
 */
@Path("/console/schedule")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class SchedulerResource implements JobConstants {

    private static final Logger logger = LoggerFactory.getLogger(SchedulerResource.class);
    private JobManager jobManager;

    private ObjectMapper mapper = new ObjectMapper();

    @GET
    @Path("/list")
    public List<JobUI> getScheduledJobs() {
        List<JobUI> result = null;

        Collection<Long> taskIds = jobManager.getJobIds();
        if (taskIds!=null && !taskIds.isEmpty()) {
            result = new ArrayList<>();
            for (Long id: taskIds) {
                JobVO jobVO = jobManager.getJob(id);
                if (jobVO != null) {
                    JobUI jobUI = new JobUI(jobVO);
                    if (jobVO.getStatus() == STATUS_ACTIVE) {
                        jobUI.nextExecutionTime = getNextExecutionTime(jobVO.getCron());
                        jobUI.local = jobManager.isActive(jobVO);
                    }
                    result.add(jobUI);
                }
            }
        }

        return result;
    }

    @GET
    @Path("/node_count")
    public Integer getNodesCount(){
        Collection<Long> jobIds = jobManager.getScheduledJobIds();
        int size = jobIds!=null? jobIds.size(): 0;
        return size;
    }

    @GET
    @Path("/card")
    public JobVO getJob(@QueryParam("id") Optional<Long> idOpt) {
        long id = idOpt.or(-1l);
        JobVO jobVO = jobManager.getJob(id);
        logger.debug("JobVO getted by id[{}] is [{}]", id, jobVO);
        return jobVO;
    }

    /**
     * POJO wrapper for UI representation of JobVO with additional fields
     */
    public static class JobUI implements Serializable {
        protected Date nextExecutionTime;
        protected boolean local=false;
        protected JobVO job;

        public JobUI(JobVO job) {
            this.job = job;
        }

        public JobVO getJob() {
            return job;
        }

        public Date getNextExecutionTime() {
            return nextExecutionTime;
        }

        public boolean isLocal() {
            return local;
        }
    }

    @PUT
    @Path("/create")
    public Long createSchedulerJob(@QueryParam("jobId")Optional<Long> jobIdOpt, CreateJobCommand command) {
        JobVO job = null;
        try {
            job = getValidJob(jobIdOpt, command);
            logger.debug("Creating scheduled task for jobIdOpt [{}], command[{}]", jobIdOpt, command);
            long id = jobManager.addJob(job);
            logger.debug("Scheduled task for command[{}] added with id[{}]", command, id);
            return id;

        } catch (Exception e) {
            logger.error("Unexpected error at create for job["+job+"]", e);
            throw new WebApplicationException(e);
        }

    }

    @PUT
    @Path("/update")
    public void updateScheduledTask(@QueryParam("jobId")Optional<Long> jobIdOpt, CreateJobCommand command) {
        JobVO job = null;
        try {
            job = getValidJob(jobIdOpt, command);
            jobManager.updateJob(job);
            logger.debug("Scheduled task with id[{}], name[{}] updated", job.getId(), job.getName());

        } catch (Exception e) {
            logger.error("Unexpected error at update for job["+job+"]", e);
            throw new WebApplicationException(e);
        }

    }

    protected JobVO getValidJob (Optional<Long> jobIdOpt, CreateJobCommand command) {
        long jobId = jobIdOpt.or(-1l);

        JobVO job = new JobVO();
        job.setId(jobId);
        job.setCron(command.getCron());
        job.setQueueLimit(command.getQueueLimit());
        job.setMaxErrors(command.getMaxErrors());
        job.setName(command.getName());
        job.setTask(createTask(command));
        job.setStatus(STATUS_INACTIVE);//modification should be applied only for inactive tasks
        validateJob(job);

        return job;
    }

    public void validateJob(JobVO job) {
        try {
            if (job == null) {
                throw new IllegalArgumentException("Job cannot be null");
            }
            CronExpression.validateExpression(job.getCron());
            if (!StringUtils.hasText(job.getName())) {
                throw new IllegalArgumentException("Job name cannot be empty");
            }
            if (job.getId()>=0 && JobConstants.STATUS_ACTIVE == jobManager.getJobStatus(job.getId())) {
                throw new IllegalArgumentException("Cannot modify active job["+job.getId()+"]!");
            }
            if (job.getTask() == null) {
                throw new IllegalArgumentException("Job cannot contain null TaskContainer!");
            } else {
                if (!StringUtils.hasText(job.getTask().getActorId())) {
                    throw new IllegalArgumentException("Job cannot contain empty Actor ID!");
                }
                if (!StringUtils.hasText(job.getTask().getMethod())) {
                    throw new IllegalArgumentException("Job cannot contain empty Method field!");
                } else if(job.getTask().getMethod().contains(" ")) {
                    throw new IllegalArgumentException("Job's method cannot contain spaces!");
                }
            }

        } catch (Throwable e) {
            logger.error("Job validation exception, job ["+job+"]", e);
            throw new WebApplicationException(e);
        }
    }

    @POST
    @Path("/action/{action}")
    public void processTask(@PathParam("action")String action, @QueryParam("id") Long id) {
        logger.debug("Sending schedule message for id [{}]", id);

        if(Action.ACTIVATE.getValue().equals(action)) {
            jobManager.startJob(id);

        } else if (Action.DEACTIVATE.getValue().equals(action)) {
            jobManager.stopJob(id);

        } else if (Action.DELETE.getValue().equals(action)) {
            jobManager.removeJob(id);

        } else {
            logger.error("Unsupported combination of method[POST] and action[" + action + "].");
            throw new WebApplicationException(Status.NOT_ACCEPTABLE);
        }
    }

    public TaskContainer createTask(CreateJobCommand command) {
        UUID guid = UUID.randomUUID();
        TaskType type = command.getTaskType() != null? command.getTaskType(): TaskType.DECIDER_START;
        long startTime = -1; // for scheduled task start time must be -1

        return new TaskContainer(guid, guid, command.getMethod(), command.getActorId(),
                type, startTime, JobConstants.DEFAULT_NUMBER_OF_ATTEMPTS,
                getTaskArguments(guid, command.getArgs()), null, false, null);
    }

    public ArgContainer[] getTaskArguments(UUID taskId, ArgVO[] args) {
        ArgContainer[] result = null;
        if (args != null && args.length>0) {
            int size = args.length;
            result = new ArgContainer[size];
            for(int i = 0; i<size; i++) {
                ArgVO arg = args[i];
                ArgContainer ac = new ArgContainer();
                ac.setValueType(ArgContainer.ValueType.PLAIN);
                ac.setPromise(false);
                ac.setReady(true);
                ac.setTaskId(taskId);

                populateArgContainerValue(ac, arg.getType(), arg.getValue());

                result[i] = ac;
                logger.debug("Resulting task [{}] argument is[{}]", taskId, ac);
            }
        }

        return result;
    }

    public void populateArgContainerValue(ArgContainer ac, String valueType, String value) {
        Class valueClass = null;
        try {
            if (StringUtils.hasText(valueType)) {
                valueClass = Thread.currentThread().getContextClassLoader().loadClass("java.lang." + capFirst(valueType.trim().toLowerCase()));
            }

            if (valueClass != null) {
                ac.setDataType(valueClass.getName());
                Object valueAsObject = valueClass.getConstructor(String.class).newInstance(value);
                ac.setJSONValue(mapper.writeValueAsString(valueAsObject));
            } else {
                ac.setDataType(null);
                ac.setJSONValue(null);
            }

        } catch(Exception e) {
            String message = "Cannot populate argument["+ac+"] value["+value+"] with type ["+valueType+"]";
            logger.error(message, e);
            throw new IllegalArgumentException(message);
        }

    }

    public static String capFirst(String target) {
        if (target!=null && target.length()>0) {
            return target.substring(0, 1).toUpperCase() + target.substring(1);
        } else {
            return target;
        }
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
    public void setJobManager(JobManager jobManager) {
        this.jobManager = jobManager;
    }
}
