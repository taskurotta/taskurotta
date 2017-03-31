package ru.taskurotta.service.pg.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.support.JdbcDaoSupport;
import org.springframework.util.StringUtils;
import ru.taskurotta.exception.ServiceCriticalException;
import ru.taskurotta.service.console.model.*;
import ru.taskurotta.service.pg.IdReturningCallback;
import ru.taskurotta.service.storage.InterruptedTasksService;
import ru.taskurotta.transport.utils.TransportUtils;

import java.sql.*;
import java.util.*;

public class PgInterruptedTaskService extends JdbcDaoSupport implements InterruptedTasksService {

    private static final Logger logger = LoggerFactory.getLogger(PgInterruptedTaskService.class);

    protected static final String SQL_CREATE_ITD_TASK = "INSERT INTO TSK_INTERRUPTED_TASKS ( " +
            "TASK_ID, PROCESS_ID, ACTOR_ID, STARTER_ID, CREATION_DATE, TIME, ERROR_MESSAGE, ERROR_CLASS_NAME, STACK_TRACE, MESSAGE_FULL " +
            ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) ON CONFLICT ON CONSTRAINT UNIQUE_TASK " +
            "DO UPDATE " +
            "SET ACTOR_ID = ?, STARTER_ID = ?, CREATION_DATE = ?, TIME = ?, ERROR_MESSAGE = ?, ERROR_CLASS_NAME = ?, STACK_TRACE = ?, MESSAGE_FULL = ? " +
            "RETURNING ID ";

    protected static final String SQL_LIST_ALL = "SELECT TASK_ID, PROCESS_ID, ACTOR_ID, STARTER_ID, CREATION_DATE, TIME, ERROR_MESSAGE, ERROR_CLASS_NAME FROM TSK_INTERRUPTED_TASKS ";

    protected static final String SQL_DELETE_ITD_TASK = "DELETE FROM TSK_INTERRUPTED_TASKS WHERE PROCESS_ID = ? AND TASK_ID = ? ";

    protected static final String SQL_GET_STACK_TRACE = "SELECT STACK_TRACE FROM TSK_INTERRUPTED_TASKS WHERE PROCESS_ID = ? AND TASK_ID = ? ";

    protected static final String SQL_GET_MESSAGE = "SELECT MESSAGE_FULL FROM TSK_INTERRUPTED_TASKS WHERE PROCESS_ID = ? AND TASK_ID = ? ";

    protected static final String SQL_GROUP_COUNTERS = " count(distinct STARTER_ID) as starters, count(distinct ACTOR_ID) as actors, count(distinct ERROR_CLASS_NAME) as errors, count(id) as total from TSK_INTERRUPTED_TASKS ";

    protected static final String SQL_LIST_TASK_IDENTIFIERS = "SELECT TASK_ID, PROCESS_ID FROM TSK_INTERRUPTED_TASKS ";

    protected static final String SQL_LIST_PROCESS_IDENTIFIERS = "SELECT DISTINCT(PROCESS_ID) as pid FROM TSK_INTERRUPTED_TASKS ";

    protected static final String SQL_DELETE_BY_PROCESS_ID = "DELETE FROM TSK_INTERRUPTED_TASKS WHERE PROCESS_ID = ? ";


    protected RowMapper<InterruptedTask> itdTaskRowMapper = (rs, i) -> {
        InterruptedTask result = new InterruptedTask();

        result.setTaskId(UUID.fromString(rs.getString("TASK_ID")));
        result.setErrorClassName(rs.getString("ERROR_CLASS_NAME"));
        result.setProcessId(UUID.fromString(rs.getString("PROCESS_ID")));
        result.setErrorMessage(rs.getString("ERROR_MESSAGE"));
        result.setActorId(rs.getString("ACTOR_ID"));
        result.setStarterId(rs.getString("STARTER_ID"));
        result.setTime(rs.getLong("TIME"));

        return result;
    };

    protected RowMapper<TasksGroupVO> taskGroupMapper = (rs, rowNum) -> {
        TasksGroupVO result = new TasksGroupVO();
        result.setName(rs.getString("name"));
        result.setActorsCount(rs.getInt("actors"));
        result.setExceptionsCount(rs.getInt("errors"));
        result.setStartersCount(rs.getInt("starters"));
        result.setTotal(rs.getInt("total"));
        return result;
    };

    protected RowMapper<TaskIdentifier> taskIdentifierMapper = (rs, rowNum) -> {
        TaskIdentifier result = new TaskIdentifier();
        result.setProcessId(rs.getString("PROCESS_ID"));
        result.setTaskId(rs.getString("TASK_ID"));
        return result;
    };

    protected RowMapper<UUID> processIdMapper = (rs, rowNum) -> UUID.fromString(rs.getString("pid"));


    @Override
    public void save(InterruptedTask itdTask, String fullMessage, String stackTrace) {
        try {
            Long id = getJdbcTemplate().execute(con -> {
                PreparedStatement ps = con.prepareStatement(SQL_CREATE_ITD_TASK);//upsert on unique (PROCESS_ID, TASK_ID)
                String taskId = itdTask.getTaskId() != null ? itdTask.getTaskId().toString() : null;
                String processId = itdTask.getProcessId() != null ? itdTask.getProcessId().toString() : null;
                String message = TransportUtils.trimToLength(itdTask.getErrorMessage(), MESSAGE_MAX_LENGTH);
                Timestamp currentTimestamp = new Timestamp(System.currentTimeMillis());

                ps.setString(1, taskId);
                ps.setString(2, processId);
                ps.setString(3, itdTask.getActorId());
                ps.setString(4, itdTask.getStarterId());
                ps.setTimestamp(5, currentTimestamp);
                ps.setLong(6, itdTask.getTime());
                ps.setString(7, message);
                ps.setString(8, itdTask.getErrorClassName());
                ps.setString(9, stackTrace);
                ps.setString(10, fullMessage);

                ps.setString(11, itdTask.getActorId());
                ps.setString(12, itdTask.getStarterId());
                ps.setTimestamp(13, currentTimestamp);
                ps.setLong(14, itdTask.getTime());
                ps.setString(15, message);
                ps.setString(16, itdTask.getErrorClassName());
                ps.setString(17, stackTrace);
                ps.setString(18, fullMessage);

                return ps;
            }, new IdReturningCallback());

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

        logger.trace("SearchSQL is[{}], params are[{}]", searchSql, parameters);
        logger.debug("Found [{}] result by command[{}] in [{}]ms", result.size(), searchCommand, (System.currentTimeMillis() - startTime));

        return result;
    }

    @Override
    public Collection<InterruptedTask> findAll() {
        Collection<InterruptedTask> result = getJdbcTemplate().query(SQL_LIST_ALL, itdTaskRowMapper);
        logger.debug("Found [{}] interrupted tasks", result.size());
        return result;
    }

    @Override
    public void delete(UUID processId, UUID taskId) {
        if (processId == null || taskId == null) {
            return;
        }

        int result = getJdbcTemplate().update(SQL_DELETE_ITD_TASK, processId.toString(), taskId.toString());
        logger.debug("Successfully deleted [{}] interrupted tasks, taskId [{}], processId[{}]", result, taskId, processId);
    }

    @Override
    public String getFullMessage(UUID processId, UUID taskId) {
        try {
            return getJdbcTemplate().queryForObject(SQL_GET_MESSAGE, String.class, processId != null ? processId.toString() : null, taskId != null ? taskId.toString() : null);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    @Override
    public String getStackTrace(UUID processId, UUID taskId) {
        try {
            return getJdbcTemplate().queryForObject(SQL_GET_STACK_TRACE, String.class, processId != null ? processId.toString() : null, taskId != null ? taskId.toString() : null);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    @Override
    public List<TasksGroupVO> getGroupList(GroupCommand command) {
        List<Object> parameters = new ArrayList<>();//order does matter
        String sql = constructGroupSql(command, parameters);

        long startTime = System.currentTimeMillis();
        List<TasksGroupVO> result = getJdbcTemplate().query(sql, parameters.toArray(), taskGroupMapper);
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

        long startTime = System.currentTimeMillis();
        List<TaskIdentifier> result = getJdbcTemplate().query(sql, parameters.toArray(), taskIdentifierMapper);
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

        long startTime = System.currentTimeMillis();
        List<UUID> list = getJdbcTemplate().query(sql, parameters.toArray(), processIdMapper);
        Set<UUID> result = new HashSet<>(list);

        logger.trace("Process ids SQL got is[{}], params are[{}]", sql, parameters);
        logger.debug("Found [{}] process UUIDs by command[{}] in [{}]ms", result.size(), command, (System.currentTimeMillis() - startTime));

        return result.isEmpty() ? null : result;
    }

    @Override
    public long deleteTasksForProcess(UUID processId) {
        if (processId == null) {
            return 0l;
        }
        return getJdbcTemplate().update(SQL_DELETE_BY_PROCESS_ID, processId.toString());
    }

    static String constructGroupSql(GroupCommand command, List<Object> parameters) {
        String column = "starter_id";//default column
        if (GroupCommand.GROUP_ACTOR.equalsIgnoreCase(command.getGroup())) {
            column = "actor_id";
        } else if (GroupCommand.GROUP_EXCEPTION.equalsIgnoreCase(command.getGroup())) {
            column = "error_class_name";
        }
        StringBuilder sb = new StringBuilder().append("SELECT ").append(column).append(" as name, ").append(SQL_GROUP_COUNTERS);

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

        if (searchCommand.getTaskId() != null) {
            sb.append((first ? " WHERE " : " AND ")).append("TASK_ID = ? ");
            parameters.add(searchCommand.getTaskId().toString());
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

}
