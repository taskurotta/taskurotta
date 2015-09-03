package ru.taskurotta.dropwizard.resources.console.broken;

import com.google.common.base.Optional;
import org.eclipse.jetty.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.util.StringUtils;
import ru.taskurotta.dropwizard.resources.console.Status;
import ru.taskurotta.service.console.model.GroupCommand;
import ru.taskurotta.service.console.model.InterruptedTask;
import ru.taskurotta.service.console.model.SearchCommand;
import ru.taskurotta.service.console.model.TaskIdentifier;
import ru.taskurotta.service.console.model.TasksGroupVO;
import ru.taskurotta.service.executor.OperationExecutor;
import ru.taskurotta.service.recovery.RecoveryService;
import ru.taskurotta.service.recovery.RestartTaskOperation;
import ru.taskurotta.service.storage.InterruptedTasksService;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

/**
 * Created: 16.10.13 11:39
 */
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path("/console/process/tasks/interrupted")
public class InterruptedTasksListResource {

    private static final Logger logger = LoggerFactory.getLogger(InterruptedTasksListResource.class);
    private static final String DATE_TEMPLATE = "dd.MM.yyyy";
    private static final String DATETIME_TEMPLATE = "dd.MM.yyyy HH:ss";

    private InterruptedTasksService interruptedTasksService;

    private OperationExecutor<RecoveryService> taskRecoveryOperationExecutor;

    private RecoveryService recoveryService;

    @GET
    @Path("/group")
    public Response getProcessesGroup(@QueryParam("dateFrom") Optional<String> dateFromOpt, @QueryParam("dateTo") Optional<String> dateToOpt,
                                      @QueryParam("starterId") Optional<String> starterIdOpt, @QueryParam("actorId") Optional<String> actorIdOpt, @QueryParam("errorClassName") Optional<String> exceptionOpt,
                                      @QueryParam("group") Optional<String> groupOpt) {
        long startTime = System.currentTimeMillis();
        GroupCommand command = convertToCommand(starterIdOpt.or(""), actorIdOpt.or(""), exceptionOpt.or(""), dateFromOpt.or(""), dateToOpt.or(""), groupOpt.or(GroupCommand.GROUP_STARTER));
        validateGroupCommand(command);
        Collection<TasksGroupVO> groups = interruptedTasksService.getGroupList(command);
        logger.debug("Process groups count got by command [{}] are [{}], total time [{}]", command, (groups != null ? groups.size() : null), System.currentTimeMillis() - startTime);
        return Response.ok(groups, MediaType.APPLICATION_JSON).build();
    }

    @GET
    @Path("/list")
    public Response getInterruptedTasksList(@QueryParam("dateFrom") Optional<String> dateFromOpt, @QueryParam("dateTo") Optional<String> dateToOpt,
                                     @QueryParam("starterId") Optional<String> starterIdOpt, @QueryParam("actorId") Optional<String> actorIdOpt, @QueryParam("errorClassName") Optional<String> exceptionOpt,
                                     @QueryParam("group") Optional<String> groupOpt) {
        GroupCommand command = convertToCommand(starterIdOpt.or(""), actorIdOpt.or(""), exceptionOpt.or(""), dateFromOpt.or(""), dateToOpt.or(""), groupOpt.or(GroupCommand.GROUP_STARTER));
        Collection<InterruptedTask> tasks = interruptedTasksService.find(command);
        logger.debug("Tasks got by command [{}] are [{}]", command, tasks);
        return Response.ok(tasks, MediaType.APPLICATION_JSON).build();
    }

    @POST
    @Path("/restart/group")
    public Response executeGroupRestart(GroupAction action) {
        GroupCommand command = convertToCommand(action.starterId, action.actorId, action.errorClassName, action.dateFrom, action.dateTo, action.group);
        logger.debug("Executing group recovery with command [{}]", command);
        Collection<TaskIdentifier> restartIds = interruptedTasksService.getTaskIdentifiers(command);
        if (restartIds != null && !restartIds.isEmpty()) {
            for (TaskIdentifier ti : restartIds) {
                taskRecoveryOperationExecutor.enqueue(new RestartTaskOperation(UUID.fromString(ti.getProcessId()), UUID.fromString(ti.getTaskId())));
            }
        }
        logger.debug("Task group restart [{}] submitted for [{}] tasks", command, restartIds!=null? restartIds.size() : 0);
        return Response.ok().build();
    }

    @POST
    @Path("/restart/task")
    public Response executeTaskRestart(final TaskIdentifier ti) {
        logger.debug("Executing task recovery with command [{}]", ti);
        taskRecoveryOperationExecutor.enqueue(new RestartTaskOperation(UUID.fromString(ti.getProcessId()), UUID.fromString(ti.getTaskId())));
        logger.debug("Task restarted [{}] ", ti);
        return Response.ok().build();
    }

    @POST
    @Path("/abort/group")
    public Status deleteGroup(GroupAction action) {
        GroupCommand command = convertToCommand(action.starterId, action.actorId, action.errorClassName, action.dateFrom, action.dateTo, action.group);
        logger.debug("Executing group abortion with command [{}]", command);
        Set<UUID> processes = interruptedTasksService.getProcessIds(command);
        int size = 0;
        if (processes!=null && !processes.isEmpty()) {
            for (UUID processId : processes) {
                long tasks = interruptedTasksService.deleteTasksForProcess(processId);
                recoveryService.abortProcess(processId);
                logger.debug("Deleted [{}] interrupted tasks for aborted process [{}]", tasks, processId);
                size++;
            }
        }
        logger.debug("Aborted [{}] processes. UUID list [{}]", size, processes);
        return new Status(HttpStatus.OK_200, "Aborted ["+size+"] processes");
    }

    @GET
    @Path("/stacktrace")
    public Status getStacktrace(@QueryParam("processId") Optional<String> processIdOpt, @QueryParam("taskId") Optional<String> taskIdOpt) {
        String result = "";
        UUID processId = processIdOpt.isPresent()? UUID.fromString(processIdOpt.get()) : null;
        UUID taskId = taskIdOpt.isPresent()? UUID.fromString(taskIdOpt.get()) : null;
        if (processId!=null && taskId != null) {
            result = interruptedTasksService.getStackTrace(processId, taskId);
        }
        return new Status(result!=null?HttpStatus.OK_200 : HttpStatus.NO_CONTENT_204, result);
    }

    @GET
    @Path("/message")
    public Status getMessage(@QueryParam("processId") Optional<String> processIdOpt, @QueryParam("taskId") Optional<String> taskIdOpt) {
        String result = "";
        UUID processId = processIdOpt.isPresent()? UUID.fromString(processIdOpt.get()) : null;
        UUID taskId = taskIdOpt.isPresent()? UUID.fromString(taskIdOpt.get()) : null;
        if (processId!=null && taskId != null) {
            result = interruptedTasksService.getFullMessage(processId, taskId);
        }
        return new Status(result!=null?HttpStatus.OK_200 : HttpStatus.NO_CONTENT_204, result);
    }

    public static class GroupAction {
        public String group;
        public String actorId;
        public String starterId;
        public String errorMessage;
        public String errorClassName;
        public String dateFrom;
        public String dateTo;
    }

    private void validateGroupCommand(GroupCommand command) {
        if (!StringUtils.hasText(command.getGroup())) {
            logger.error("Invalid group command got [" + command + "]");
            throw new WebApplicationException(new IllegalArgumentException("Group command cannot be empty"));
        }
        validateSearchCommand(command);
    }

    private void validateSearchCommand(SearchCommand command) {
        if (!StringUtils.hasText(command.getActorId())
                && (command.getProcessId() == null || !StringUtils.hasText(command.getProcessId().toString()))
                && !StringUtils.hasText(command.getErrorClassName())
                && command.getStartPeriod() < 0
                && command.getEndPeriod() < 0) {
            logger.error("Invalid search command got [" + command + "]");
            throw new WebApplicationException(new IllegalArgumentException("Search command cannot be empty"));
        }
    }

    public static GroupCommand convertToCommand(String starterId, String actorId, String exception,
                                                String dateFrom, String dateTo, String group) {
        GroupCommand result = new GroupCommand();
        appendPeriodValues(result, dateFrom, dateTo);
        result.setStarterId(starterId);
        result.setActorId(actorId);
        result.setErrorClassName(exception);
        result.setGroup(group);//default group processes by starter task

        logger.debug("Converted GroupCommand is [{}]", result);

        return result;
    }

    private static void appendPeriodValues(GroupCommand result, String dateFrom, String dateTo) {
        if (StringUtils.hasText(dateFrom) || StringUtils.hasText(dateTo)) {
            SimpleDateFormat sdf = null;
            boolean withTime = false;
            if (dateFrom.length() > 10 || dateTo.length() > 10) {
                sdf = new SimpleDateFormat(DATETIME_TEMPLATE);
                withTime = true;
            } else {
                sdf = new SimpleDateFormat(DATE_TEMPLATE);
            }
            sdf.setLenient(false);

            try {
                if (StringUtils.hasText(dateFrom)) {
                    Date dateFromDate = sdf.parse(dateFrom);
                    result.setStartPeriod(dateFromDate.getTime());
                }
                if (StringUtils.hasText(dateTo)) {
                    Date dateToDate = sdf.parse(dateTo);
                    if (!withTime) {
                        dateToDate.setTime(dateToDate.getTime() + 24 * 60 * 60 * 1000);//+one day, cause default time 00:00 cuts off current day
                    }
                    result.setEndPeriod(dateToDate.getTime());
                }
            } catch (Exception e) {
                logger.error("Cannot parse date: from[" + dateFrom + "], to[" + dateTo + "]", e);
                throw new WebApplicationException(e);
            }
        }
    }

    @Required
    public void setInterruptedTasksService(InterruptedTasksService interruptedTasksService) {
        this.interruptedTasksService = interruptedTasksService;
    }

    @Required
    public void setTaskRecoveryOperationExecutor(OperationExecutor<RecoveryService> taskRecoveryOperationExecutor) {
        this.taskRecoveryOperationExecutor = taskRecoveryOperationExecutor;
    }

    @Required
    public void setRecoveryService(RecoveryService recoveryService) {
        this.recoveryService = recoveryService;
    }
}
