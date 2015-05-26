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
import ru.taskurotta.service.console.model.InterruptedTask;
import ru.taskurotta.service.console.model.SearchCommand;
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
import java.util.List;
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

    protected static final String SQL_GET_STACK_TRACE = "SELECT STACK_TRACE FROM TSK_INTERRUPTED_TASKS WHERE PROCESS_ID = ? AND TASK_ID = ? ";

    protected static final String SQL_GET_MESSAGE = "SELECT FULL_MESSAGE FROM TSK_INTERRUPTED_TASKS WHERE PROCESS_ID = ? AND TASK_ID = ? ";

    protected LobHandler lobHandler;


    protected RowMapper<String> clobMapper = new RowMapper<String>() {

        @Override
        public String mapRow(ResultSet rs, int i) throws SQLException {
            return rs.getString(1);
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
            throw new ServiceCriticalException(errMessage);
        }

    }

    @Override
    public Collection<InterruptedTask> find(SearchCommand searchCommand) {
        List<Object> parameters = new ArrayList<>();//order does matter
        StringBuilder sb = new StringBuilder(SQL_LIST_ALL);
        boolean first = true;
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

        Collection<InterruptedTask> result;
        String searchSql = sb.toString();
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
        List<String> result = getJdbcTemplate().query(SQL_GET_MESSAGE, clobMapper, processId, taskId);//TODO: migrate to some interrupted task unique identifier
        return result!=null&&!result.isEmpty()? result.get(0) : null;
    }

    @Override
    public String getStackTrace(UUID processId, UUID taskId) {
        List<String> result = getJdbcTemplate().query(SQL_GET_STACK_TRACE, clobMapper, processId, taskId);//TODO: migrate to some interrupted task unique identifier
        return result!=null&&!result.isEmpty()? result.get(0) : null;
    }

    @Required
    public void setLobHandler(LobHandler lobHandler) {
        this.lobHandler = lobHandler;
    }

}
