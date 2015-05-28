package ru.taskurotta.service.ora.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.CallableStatementCallback;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.support.JdbcDaoSupport;
import org.springframework.jdbc.support.lob.LobCreator;
import org.springframework.jdbc.support.lob.LobHandler;
import org.springframework.util.StringUtils;
import ru.taskurotta.exception.ServiceCriticalException;
import ru.taskurotta.service.console.model.GroupCommand;
import ru.taskurotta.service.console.model.InterruptedTask;
import ru.taskurotta.service.console.model.SearchCommand;
import ru.taskurotta.service.console.model.TaskIdentifier;
import ru.taskurotta.service.console.model.TasksGroupVO;
import ru.taskurotta.service.storage.InterruptedTasksService;
import ru.taskurotta.transport.utils.TransportUtils;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Created on 19.03.2015.
 */
public class OraInterruptedTasksService extends JdbcDaoSupport implements InterruptedTasksService {

    private static final Logger logger = LoggerFactory.getLogger(OraInterruptedTasksService.class);

    protected static final String SQL_CREATE_ITD_TASK = "BEGIN " +
            "INSERT INTO TSK_INTERRUPTED_TASKS (TASK_ID, PROCESS_ID, ACTOR_ID, STARTER_ID, CREATION_DATE, TIME, ERROR_MESSAGE, ERROR_CLASS_NAME, STACK_TRACE, MESSAGE_FULL) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING ID INTO ?; " +
            "END;";

    protected static final String SQL_LIST_ALL = "SELECT TASK_ID, PROCESS_ID, ACTOR_ID, STARTER_ID, CREATION_DATE, TIME, ERROR_MESSAGE, ERROR_CLASS_NAME FROM TSK_INTERRUPTED_TASKS ";

    protected static final String SQL_DELETE_ITD_TASK = "DELETE FROM TSK_INTERRUPTED_TASKS WHERE PROCESS_ID = ? AND TASK_ID = ? ";

    protected static final String SQL_GET_STACK_TRACE = "SELECT STACK_TRACE clb FROM TSK_INTERRUPTED_TASKS WHERE PROCESS_ID = ? AND TASK_ID = ? ";

    protected static final String SQL_GET_MESSAGE = "SELECT MESSAGE_FULL clb FROM TSK_INTERRUPTED_TASKS WHERE PROCESS_ID = ? AND TASK_ID = ? ";

    protected static final String SQL_GROUP_COUNTERS = " count(distinct STARTER_ID) starters, count(distinct ACTOR_ID) actors, count(distinct ERROR_CLASS_NAME) errors, count(id) total from TSK_INTERRUPTED_TASKS ";

    protected static final String SQL_LIST_TASK_IDENTIFIERS = "SELECT TASK_ID, PROCESS_ID FROM TSK_INTERRUPTED_TASKS ";

    protected static final String SQL_LIST_PROCESS_IDENTIFIERS = "SELECT DISTINCT(PROCESS_ID) pid FROM TSK_INTERRUPTED_TASKS ";

    protected static final String SQL_DELETE_BY_PROCESS_ID = "DELETE FROM TSK_INTERRUPTED_TASKS WHERE PROCESS_ID = ? ";

    protected LobHandler lobHandler;


    protected RowMapper<String> clobMapper = new RowMapper<String>() {

        @Override
        public String mapRow(ResultSet rs, int i) throws SQLException {
            return lobHandler.getClobAsString(rs, "clb");
        }
    };

    protected RowMapper<InterruptedTask> itdTaskRowMapper = new RowMapper<InterruptedTask>() {

        @Override
        public InterruptedTask mapRow(ResultSet rs, int i) throws SQLException {
            InterruptedTask result = new InterruptedTask();

            result.setTaskId(UUID.fromString(rs.getString("TASK_ID")));
            result.setErrorClassName(rs.getString("ERROR_CLASS_NAME"));
            result.setProcessId(UUID.fromString(rs.getString("PROCESS_ID")));
            result.setErrorMessage(rs.getString("ERROR_MESSAGE"));
            result.setActorId(rs.getString("ACTOR_ID"));
            result.setStarterId(rs.getString("STARTER_ID"));
            result.setTime(rs.getLong("TIME"));

            return result;
        }
    };

    protected RowMapper<TasksGroupVO> taskGroupMapper = new RowMapper<TasksGroupVO>() {
        @Override
        public TasksGroupVO mapRow(ResultSet rs, int rowNum) throws SQLException {
            TasksGroupVO result = new TasksGroupVO();
            result.setName(rs.getString("name"));
            result.setActorsCount(rs.getInt("actors"));
            result.setExceptionsCount(rs.getInt("errors"));
            result.setStartersCount(rs.getInt("starters"));
            result.setTotal(rs.getInt("total"));
            return result;
        }
    };

    protected RowMapper<TaskIdentifier> taskIdentifierMapper = new RowMapper<TaskIdentifier>() {
        @Override
        public TaskIdentifier mapRow(ResultSet rs, int rowNum) throws SQLException {
            TaskIdentifier result = new TaskIdentifier();
            result.setProcessId(rs.getString("PROCESS_ID"));
            result.setTaskId(rs.getString("TASK_ID"));
            return result;
        }
    };

    protected RowMapper<UUID> processIdMapper = new RowMapper<UUID>() {
        @Override
        public UUID mapRow(ResultSet rs, int rowNum) throws SQLException {
            return UUID.fromString(rs.getString("pid"));
        }
    };

    @Override
    public void save(final InterruptedTask itdTask, final String message, final String stackTrace) {

        try {
            Long id = getJdbcTemplate().execute(SQL_CREATE_ITD_TASK,
                    new CallableStatementCallback<Long>() {

                        public Long doInCallableStatement(CallableStatement ps) throws SQLException, DataAccessException {
                            String taskId = itdTask.getTaskId()!=null? itdTask.getTaskId().toString(): null;
                            String processId = itdTask.getProcessId()!=null? itdTask.getProcessId().toString(): null;
                            LobCreator lobCreator = lobHandler.getLobCreator();
                            ps.setString(1, taskId);
                            ps.setString(2, processId);
                            ps.setString(3, itdTask.getActorId());
                            ps.setString(4, itdTask.getStarterId());
                            ps.setTimestamp(5, new Timestamp(new Date().getTime()));
                            ps.setLong(6, itdTask.getTime());
                            ps.setString(7, TransportUtils.trimToLength(itdTask.getErrorMessage(), MESSAGE_MAX_LENGTH));
                            ps.setString(8, itdTask.getErrorClassName());
                            lobCreator.setClobAsString(ps, 9, stackTrace);
                            lobCreator.setClobAsString(ps, 10, message);
                            ps.registerOutParameter(11, Types.BIGINT);

                            ps.execute();
                            lobCreator.close();

                            return ps.getLong(11);
                        }
                    });

            logger.debug("Created interrupted task entry with id[{}]", id);
        } catch (DataAccessException e) {
            String errMessage = "Cannot create interrupted task entry[" + itdTask + "]";
            logger.error(errMessage, e);
            throw new ServiceCriticalException(errMessage, e);
        }

    }

    @Override
    public Collection<InterruptedTask> find(SearchCommand searchCommand) {
        List<Object> parameters = new ArrayList<>();//order does matter
        String searchSql = constructSearchSql(searchCommand, parameters);

        Collection<InterruptedTask> result;
        long startTime = System.currentTimeMillis();
        try {
            result = getJdbcTemplate().query(searchSql, parameters.toArray(), itdTaskRowMapper);
        } catch (EmptyResultDataAccessException e) {
            result = Collections.emptyList();
        }

        logger.trace("SearchSQL got is[{}], params are[{}]", searchSql, parameters);
        logger.debug("Found [{}] result by command[{}] in [{}]ms", result.size(), searchCommand, (System.currentTimeMillis() - startTime));

        return result;
    }

    static String constructGroupSql(GroupCommand command, List<Object> parameters) {
        String column = "starter_id";//default column
        if (GroupCommand.GROUP_ACTOR.equalsIgnoreCase(command.getGroup())) {
            column = "actor_id";
        } else if (GroupCommand.GROUP_EXCEPTION.equalsIgnoreCase(command.getGroup())) {
            column = "error_class_name";
        }
        StringBuilder sb = new StringBuilder().append("SELECT ").append(column).append(" name, ").append(SQL_GROUP_COUNTERS);

        appendFilterCondition(sb, command, parameters, true);

        sb.append(" GROUP by ").append(column);

        return sb.toString();
    }

    static String constructSearchSql(SearchCommand searchCommand, List<Object> parameters) {
        StringBuilder sb = new StringBuilder(SQL_LIST_ALL);
        appendFilterCondition(sb, searchCommand, parameters, true);
        return sb.toString();
    }

    static void appendFilterCondition(StringBuilder sb, SearchCommand searchCommand, List<Object> parameters, boolean first) {
        if (searchCommand.getProcessId() != null) {
            sb.append((first ? " WHERE " : " AND ")).append("PROCESS_ID = ? ");
            parameters.add(searchCommand.getProcessId().toString());
            first = false;
        }

        if (StringUtils.hasText(searchCommand.getActorId())) {
            sb.append((first ? " WHERE " : " AND ")).append("ACTOR_ID LIKE ? ");
            parameters.add(searchCommand.getActorId() + "%");
            first = false;
        }

        if (StringUtils.hasText(searchCommand.getStarterId())) {
            sb.append((first ? " WHERE " : " AND ")).append("STARTER_ID LIKE ? ");
            parameters.add(searchCommand.getStarterId() + "%");
            first = false;
        }

        if (StringUtils.hasText(searchCommand.getErrorClassName())) {
            sb.append((first ? " WHERE " : " AND ")).append("ERROR_CLASS_NAME LIKE ? ");
            parameters.add(searchCommand.getErrorClassName() + "%");
            first = false;
        }

        if (StringUtils.hasText(searchCommand.getErrorMessage())) {
            sb.append((first ? " WHERE " : " AND ")).append("ERROR_MESSAGE LIKE ? ");
            parameters.add(searchCommand.getErrorMessage() + "%");
            first = false;
        }

        if (searchCommand.getEndPeriod() > 0) {
            sb.append((first ? " WHERE " : " AND ")).append("TIME < ? ");
            parameters.add(searchCommand.getEndPeriod());
            first = false;
        }

        if (searchCommand.getStartPeriod() > 0) {
            sb.append((first ? " WHERE " : " AND ")).append("TIME > ? ");
            parameters.add(searchCommand.getStartPeriod());
            first = false;
        }
    }

    @Override
    public Collection<InterruptedTask> findAll() {
        Collection<InterruptedTask> result;
        try {
            result = getJdbcTemplate().query(SQL_LIST_ALL, itdTaskRowMapper);
        } catch (EmptyResultDataAccessException e) {
            result = Collections.emptyList();//nothing found
        }

        logger.debug("Found [{}] interrupted tasks", result.size());
        return result;
    }

    @Override
    public void delete(UUID processId, UUID taskId) {

        if (processId == null || taskId==null) {
            return;
        }

        int result = getJdbcTemplate().update(SQL_DELETE_ITD_TASK, processId.toString(), taskId.toString());
        logger.debug("Successfully deleted [{}] interrupted tasks, taskId [{}], processId[{}]", result, taskId, processId);
    }

    @Override
    public String getFullMessage(UUID processId, UUID taskId) {
        List<String> result = getJdbcTemplate().query(SQL_GET_MESSAGE, clobMapper, processId!=null? processId.toString(): null, taskId!=null? taskId.toString(): null);//TODO: migrate to some interrupted task unique identifier
        return result!=null&&!result.isEmpty()? result.get(0) : null;
    }

    @Override
    public String getStackTrace(UUID processId, UUID taskId) {
        List<String> result = getJdbcTemplate().query(SQL_GET_STACK_TRACE, clobMapper, processId!=null? processId.toString(): null, taskId!=null? taskId.toString(): null);//TODO: migrate to some interrupted task unique identifier
        return result!=null&&!result.isEmpty()? result.get(0) : null;
    }

    @Override
    public List<TasksGroupVO> getGroupList(GroupCommand command) {
        List<Object> parameters = new ArrayList<>();//order does matter
        String sql = constructGroupSql(command, parameters);

        List<TasksGroupVO> result;
        long startTime = System.currentTimeMillis();
        try {
            result = getJdbcTemplate().query(sql, parameters.toArray(), taskGroupMapper);
        } catch (EmptyResultDataAccessException e) {
            result = Collections.emptyList();//nothing found
        }

        logger.trace("Group SQL got is[{}], params are[{}]", sql, parameters);
        logger.debug("Found [{}] result by command[{}] in [{}]ms", result.size(), command, (System.currentTimeMillis() - startTime));

        return result;
    }

    @Override
    public Collection<TaskIdentifier> getTaskIdentifiers(GroupCommand command) {
        List<Object> parameters = new ArrayList<>();//order does matter
        StringBuilder sb = new StringBuilder(SQL_LIST_TASK_IDENTIFIERS);
        if (GroupCommand.GROUP_ACTOR.equalsIgnoreCase(command.getGroup())) {
            sb.append("WHERE ACTOR_ID = ? ");
            parameters.add(command.getActorId());
        } else if (GroupCommand.GROUP_EXCEPTION.equalsIgnoreCase(command.getGroup())) {
            sb.append("WHERE ERROR_CLASS_NAME = ? ");
            parameters.add(command.getErrorClassName());
        } else { //STARTERS by default
            sb.append("WHERE STARTER_ID = ? ");
            parameters.add(command.getStarterId());
        }

        appendFilterCondition(sb, command, parameters, false);

        String sql = sb.toString();

        List<TaskIdentifier> result;
        long startTime = System.currentTimeMillis();
        try {
            result = getJdbcTemplate().query(sql, parameters.toArray(), taskIdentifierMapper);
        } catch (EmptyResultDataAccessException e) {
            result = Collections.emptyList();//nothing found
        }

        logger.trace("Task ifdentifiers SQL got is[{}], params are[{}]", sql, parameters);
        logger.debug("Found [{}] result by command[{}] in [{}]ms", result.size(), command, (System.currentTimeMillis() - startTime));

        return result;
    }

    @Override
    public Set<UUID> getProcessIds(GroupCommand command) {
        List<Object> parameters = new ArrayList<>();//order does matter
        StringBuilder sb = new StringBuilder(SQL_LIST_PROCESS_IDENTIFIERS);
        if (GroupCommand.GROUP_ACTOR.equalsIgnoreCase(command.getGroup())) {
            sb.append("WHERE ACTOR_ID = ? ");
            parameters.add(command.getActorId());
        } else if (GroupCommand.GROUP_EXCEPTION.equalsIgnoreCase(command.getGroup())) {
            sb.append("WHERE ERROR_CLASS_NAME = ? ");
            parameters.add(command.getErrorClassName());
        } else { //STARTERS by default
            sb.append("WHERE STARTER_ID = ? ");
            parameters.add(command.getStarterId());
        }

        appendFilterCondition(sb, command, parameters, false);

        String sql = sb.toString();

        List<UUID> list;
        long startTime = System.currentTimeMillis();
        try {
            list = getJdbcTemplate().query(sql, parameters.toArray(), processIdMapper);
        } catch (EmptyResultDataAccessException e) {
            list = Collections.emptyList();//nothing found
        }
        Set<UUID> result = new HashSet<>(list);

        logger.trace("Process ids SQL got is[{}], params are[{}]", sql, parameters);
        logger.debug("Found [{}] process UUIDs by command[{}] in [{}]ms", result.size(), command, (System.currentTimeMillis() - startTime));

        return result.isEmpty()? null : result;
    }

    @Override
    public long deleteTasksForProcess(UUID processId) {
        if (processId==null) {
            return 0l;
        }
        return getJdbcTemplate().update(SQL_DELETE_BY_PROCESS_ID, processId.toString());
    }

    @Required
    public void setLobHandler(LobHandler lobHandler) {
        this.lobHandler = lobHandler;
    }

}
