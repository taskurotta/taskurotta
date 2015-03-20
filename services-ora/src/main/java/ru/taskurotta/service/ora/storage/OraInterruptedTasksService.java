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

import java.sql.*;
import java.util.*;
import java.util.Date;

/**
 * Created on 19.03.2015.
 */
public class OraInterruptedTasksService extends JdbcDaoSupport implements InterruptedTasksService {

    private static final Logger logger = LoggerFactory.getLogger(OraInterruptedTasksService.class);

    protected static final String SQL_CREATE_ITD_TASK = "BEGIN " +
            "INSERT INTO TSK_INTERRUPTED_TASK (TASK_ID, PROCESS_ID, ACTOR_ID, STARTER_ID, CREATION_DATE, TIME, ERROR_MESSAGE, ERROR_CLASS_NAME, STACK_TRACE) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING ID INTO ?; " +
            "END;";

    protected static final String SQL_LIST_ALL = "SELECT TASK_ID, PROCESS_ID, ACTOR_ID, STARTER_ID, CREATION_DATE, TIME, ERROR_MESSAGE, ERROR_CLASS_NAME, STACK_TRACE FROM TSK_INTERRUPTED_TASK ";

    protected static final String SQL_DELETE_ITD_TASK = "DELETE FROM TSK_INTERRUPTED_TASK WHERE PROCESS_ID = ? AND TASK_ID = ? ";

    protected LobHandler lobHandler;

    protected RowMapper<InterruptedTask> itdTaskRowMapper = new RowMapper<InterruptedTask>() {

        @Override
        public InterruptedTask mapRow(ResultSet rs, int i) throws SQLException {
            InterruptedTask result = new InterruptedTask();

            result.setTaskId(UUID.fromString(rs.getString("TASK_ID")));
            result.setErrorClassName(rs.getString("ERROR_CLASS_NAME"));
            result.setProcessId(UUID.fromString(rs.getString("PROCESS_ID")));
            result.setStackTrace(lobHandler.getClobAsString(rs, "STACK_TRACE"));
            result.setErrorMessage(rs.getString("ERROR_MESSAGE"));
            result.setActorId(rs.getString("ACTOR_ID"));
            result.setStarterId(rs.getString("STARTER_ID"));
            result.setTime(rs.getLong("TIME"));

            return result;
        }
    };

    @Override
    public void save(final InterruptedTask itdTask) {

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
                            ps.setString(7, itdTask.getErrorMessage());
                            ps.setString(8, itdTask.getErrorClassName());
                            lobCreator.setClobAsString(ps, 9, itdTask.getStackTrace());

                            ps.registerOutParameter(10, Types.BIGINT);

                            ps.execute();
                            lobCreator.close();

                            return ps.getLong(10);
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


    @Required
    public void setLobHandler(LobHandler lobHandler) {
        this.lobHandler = lobHandler;
    }

}
