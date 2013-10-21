package ru.taskurotta.dropwizard.resources.console.broken;

import com.google.common.base.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.util.StringUtils;
import ru.taskurotta.backend.process.BrokenProcessBackend;
import ru.taskurotta.backend.process.BrokenProcessVO;
import ru.taskurotta.backend.process.GroupCommand;
import ru.taskurotta.backend.process.ProcessGroupVO;
import ru.taskurotta.dropwizard.resources.Action;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * User: dimadin
 * Date: 16.10.13 11:39
 */
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path("/console/process/broken/{action}")
public class BrokenProcessListResource {

    private static final Logger logger = LoggerFactory.getLogger(BrokenProcessListResource.class);
    private static final String DATE_TEMPLATE = "dd.MM.yyyy";
    private static final String DATETIME_TEMPLATE = "dd.MM.yyyy HH:ss";


    private BrokenProcessBackend brokenProcessBackend;

    @GET
    public Response getProcesses(@PathParam("action") String action, @QueryParam("dateFrom")Optional<String> dateFromOpt, @QueryParam("dateTo")Optional<String> dateToOpt,
                                 @QueryParam("starterId")Optional<String> starterIdOpt, @QueryParam("actorId")Optional<String> actorIdOpt, @QueryParam("exception")Optional<String> exceptionOpt,
                                 @QueryParam("group")Optional<String> groupOpt) {

        if (Action.GROUP.getValue().equals(action)) {
            GroupCommand command = convertToCommand(starterIdOpt, actorIdOpt, exceptionOpt, dateFromOpt, dateToOpt, groupOpt);
            Collection <ProcessGroupVO> groups =  getGroupList(command);
            logger.debug("Process groups got by command [{}] are [{}]", command, groups);
            return Response.ok(groups, MediaType.APPLICATION_JSON).build();

        } else if (Action.LIST.getValue().equals(action)) {
            GroupCommand command = convertToCommand(starterIdOpt, actorIdOpt, exceptionOpt, dateFromOpt, dateToOpt, groupOpt);
            Collection<BrokenProcessVO> processes = brokenProcessBackend.find(command);
            logger.debug("Processes got by command [{}] are [{}]", command, processes);
            return Response.ok(processes, MediaType.APPLICATION_JSON).build();

        } else {
            logger.error("Unsupported action["+action+"]");
            return Response.serverError().build();

        }

    }

    public static GroupCommand convertToCommand(Optional<String> starterIdOpt, Optional<String> actorIdOpt, Optional<String> exceptionOpt,
                                         Optional<String> dateFromOpt, Optional<String> dateToOpt, Optional<String> groupOpt) {
        String dateFrom = dateFromOpt.or("");
        String dateTo = dateToOpt.or("");
        GroupCommand result = new GroupCommand();

        if (StringUtils.hasText(dateFrom) || StringUtils.hasText(dateTo)) {
            SimpleDateFormat sdf = null;
            if (dateFrom.length()>10 || dateTo.length()>10) {
                sdf = new SimpleDateFormat(DATETIME_TEMPLATE);
            } else {
                sdf = new SimpleDateFormat(DATE_TEMPLATE);
            }
            sdf.setLenient(false);

            try {
                if (StringUtils.hasText(dateFrom)) {
                    Date dateFromDate = sdf.parse(dateFrom);
                    result.setStartPeriod(dateFromDate.getTime());
                }
                if (StringUtils.hasText(dateFrom)) {
                    Date dateToDate = sdf.parse(dateTo);
                    result.setEndPeriod(dateToDate.getTime());
                }
            } catch (Exception e) {
                logger.error("Cannot parse date: from["+dateFrom+"], to["+dateTo+"]", e);
                throw new WebApplicationException(e);
            }
        }

        result.setStartActorId(starterIdOpt.or(""));
        result.setBrokenActorId(actorIdOpt.or(""));
        result.setErrorClassName(exceptionOpt.or(""));
        result.setGroup(groupOpt.or(GroupCommand.GROUP_STARTER));//default group processes by starter task

        logger.debug("Converted GroupCommand is [{}]", result);

        return result;
    }

    public List<ProcessGroupVO> getGroupList(GroupCommand command) {
        List<ProcessGroupVO> result = null;
        Collection<BrokenProcessVO> processes = brokenProcessBackend.find(command);

        if (processes != null && !processes.isEmpty()) {
            Map<String, Collection<BrokenProcessVO>> groupedProcesses = groupProcessList(processes, command.getGroup());
            result = convertToGroupsList(groupedProcesses, command);
        }

        return result;
    }


    private Map<String, Collection<BrokenProcessVO>> groupProcessList(Collection<BrokenProcessVO> processes, String groupType) {
        Map<String, Collection<BrokenProcessVO>> result = new HashMap<>();

        if (processes != null && !processes.isEmpty()) {

            if (GroupCommand.GROUP_STARTER.equals(groupType)) {
                for (BrokenProcessVO bp: processes) {
                    Collection<BrokenProcessVO> coll = result.get(bp.getStartActorId());
                    if (coll == null) {
                        coll = new ArrayList<>();
                    }
                    coll.add(bp);
                    result.put(bp.getStartActorId(), coll);
                }
            } else if (GroupCommand.GROUP_ACTOR.equals(groupType)) {
                for (BrokenProcessVO bp: processes) {
                    Collection<BrokenProcessVO> coll = result.get(bp.getBrokenActorId());
                    if (coll == null) {
                        coll = new ArrayList<>();
                    }
                    coll.add(bp);
                    result.put(bp.getBrokenActorId(), coll);
                }
            } else if (GroupCommand.GROUP_EXCEPTION.equals(groupType)) {
                for (BrokenProcessVO bp: processes) {
                    Collection<BrokenProcessVO> coll = result.get(bp.getErrorClassName());
                    if (coll == null) {
                        coll = new ArrayList<>();
                    }
                    coll.add(bp);
                    result.put(bp.getErrorClassName(), coll);
                }
            } else {
                logger.error("Unsupported groupType["+groupType+"]");
            }

        }

        return result;
    }

    private List<ProcessGroupVO> convertToGroupsList(Map<String, Collection<BrokenProcessVO>> groupedProcesses, GroupCommand command) {
        List<ProcessGroupVO> result = null;
        if (groupedProcesses!=null && !groupedProcesses.isEmpty()) {
            result = new ArrayList<>();
            for (String name: groupedProcesses.keySet()) {
                Collection<BrokenProcessVO> groupItems = groupedProcesses.get(name);
                ProcessGroupVO group = convertToGroup(groupItems, name);
                result.add(group);
            }
        }
        return result;
    }

    private static ProcessGroupVO convertToGroup(Collection<BrokenProcessVO> members, String name) {
        ProcessGroupVO group= new ProcessGroupVO();
        Set<String> actorsDiffs = new HashSet<>();
        Set<String> startersDiffs = new HashSet<>();
        Set<String> exceptionsDiffs = new HashSet<>();
        Set<String> processIds = new HashSet<>();
        if (members!=null && !members.isEmpty()) {
            for(BrokenProcessVO bp: members) {
                actorsDiffs.add(bp.getBrokenActorId());
                startersDiffs.add(bp.getStartActorId());
                exceptionsDiffs.add(bp.getErrorClassName());
                processIds.add(bp.getProcessId());
            }
        }
        group.setName(name);
        group.setStartersCount(startersDiffs.size());
        group.setActorsCount(actorsDiffs.size());
        group.setExceptionsCount(exceptionsDiffs.size());
        group.setTotal(processIds.size());
        group.setProcessIds(processIds);

        return group;
    }

    @Required
    public void setBrokenProcessBackend(BrokenProcessBackend brokenProcessBackend) {
        this.brokenProcessBackend = brokenProcessBackend;
    }

}
