package ru.taskurotta.service.pg.storage;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.support.JdbcDaoSupport;
import ru.taskurotta.exception.ServiceCriticalException;
import ru.taskurotta.service.common.ResultSetCursor;
import ru.taskurotta.service.console.model.GenericPage;
import ru.taskurotta.service.console.model.Process;
import ru.taskurotta.service.console.retriever.ProcessInfoRetriever;
import ru.taskurotta.service.console.retriever.command.ProcessSearchCommand;
import ru.taskurotta.service.hz.server.HzTaskServer;
import ru.taskurotta.service.pg.PgQueryUtils;
import ru.taskurotta.service.pg.UuidResultSetCursor;
import ru.taskurotta.service.storage.IdempotencyKeyViolation;
import ru.taskurotta.service.storage.ProcessService;
import ru.taskurotta.transport.model.TaskConfigContainer;
import ru.taskurotta.transport.model.TaskContainer;
import ru.taskurotta.transport.model.TaskOptionsContainer;
import ru.taskurotta.transport.utils.TransportUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;

public class PgProcessService extends JdbcDaoSupport implements ProcessService, ProcessInfoRetriever {

    private static final Logger logger = LoggerFactory.getLogger(PgProcessService.class);

    private static final String LOCK_PROCESS_MAP_NAME = HzTaskServer.class.getName() + "#lockProcessMap";
    private static final String IDEMPOTENCY_PROCESS_MAP_NAME = HzTaskServer.class.getName() + "#idempotencyProcessMap";

    public static final String WILDCARD = "%";

    protected static final String SQL_GET_PROCESS_CNT_BY_STATE =
            "SELECT COUNT(PROCESS_ID) AS cnt FROM TSK_PROCESS WHERE STATE = ? ";

    protected static final String SQL_FIND_INCOMPLETE_PROCESSES =
            "SELECT PROCESS_ID FROM TSK_PROCESS WHERE STATE = ? AND START_TIME < ? ";

    protected static final String SQL_GET_PROCESS_LIST =
            "SELECT PROCESS_ID, START_TASK_ID, CUSTOM_ID, START_TIME, END_TIME, STATE, RETURN_VALUE, START_JSON FROM TSK_PROCESS ";

    protected static final String SQL_GET_PROCESS_LIST_CNT =
            "SELECT COUNT(PROCESS_ID) FROM TSK_PROCESS ";

    protected static final String SQL_FIND_LOST_PROCESSES =
            "SELECT PROCESS_ID FROM TSK_PROCESS WHERE (STATE = ? AND START_TIME < ?) OR (STATE = ? AND END_TIME < ?) ";

    protected static final String SQL_GET_PROCESS_CNT_BY_STATE_AND_STARTER_ID =
            "SELECT COUNT(PROCESS_ID) AS cnt FROM TSK_PROCESS WHERE STATE = ? AND ACTOR_ID = ? ";

    private IMap<UUID, ?> lockProcessMap;
    private IMap<String, ?> idempotencyProcessMap;

    protected RowMapper<Process> processMapper = (rs, rowNum) -> {
        Process process = new Process();
        process.setProcessId(UUID.fromString(rs.getString("process_id")));
        process.setStartTaskId(UUID.fromString(rs.getString("start_task_id")));
        process.setCustomId(rs.getString("custom_id"));
        process.setStartTime(rs.getLong("start_time"));
        process.setEndTime(rs.getLong("end_time"));
        process.setState(rs.getInt("state"));
        process.setReturnValue(rs.getString("return_value"));
        process.setStartTask(PgQueryUtils.readValue(rs.getString("start_json"), TaskContainer.class));
        return process;
    };

    public PgProcessService(HazelcastInstance hzInstance, DataSource dataSource) {
        lockProcessMap = hzInstance.getMap(LOCK_PROCESS_MAP_NAME);
        idempotencyProcessMap = hzInstance.getMap(IDEMPOTENCY_PROCESS_MAP_NAME);
        setDataSource(dataSource);
    }

    @Override
    public void lock(UUID processId) {
        lockProcessMap.lock(processId);
    }

    @Override
    public void unlock(UUID processId) {
        lockProcessMap.unlock(processId);
    }

    @Override
    public void startProcess(TaskContainer task) {
        String idempotencyKey = Optional.ofNullable(task.getOptions())
                .map(TaskOptionsContainer::getTaskConfigContainer)
                .map(TaskConfigContainer::getIdempotencyKey)
                .orElse(null);

        if (idempotencyKey != null) {
            if (idempotencyProcessMap.tryLock(idempotencyKey)) {
                try {
                    getJdbcTemplate().update("INSERT INTO TSK_PROCESS_IDEMPOTENCY (IDEMPOTENCY_KEY, PROCESS_ID, START_TIME) VALUES (?, ?, ?)",
                            idempotencyKey, task.getProcessId().toString(), new Date().getTime());
                } catch (DuplicateKeyException ex) {
                    throw new IdempotencyKeyViolation();
                } finally {
                    idempotencyProcessMap.unlock(idempotencyKey);
                }
            } else {
                throw new IdempotencyKeyViolation();
            }
        }

        try {
            logger.debug("Try to start process with task [{}]", task);
            getJdbcTemplate().update("INSERT INTO TSK_PROCESS (PROCESS_ID, START_TASK_ID, CUSTOM_ID, START_TIME, STATE, START_JSON, ACTOR_ID, TASK_LIST) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                    task.getProcessId().toString(),  task.getTaskId().toString(),
                    TransportUtils.getCustomId(task), new Date().getTime(),
                    Process.ACTIVE, PgQueryUtils.asJsonbObject(task),
                    task.getActorId(), TransportUtils.getTaskList(task));
        } catch (Throwable ex) {
            String message = "DB exception on starting process by task["+task+"]";
            logger.error(message, ex);
            throw new ServiceCriticalException(message, ex);
        }
    }

    @Override
    public void finishProcess(UUID processId, String returnValue) {
        try {
            logger.debug("Try to finish process id [{}] with value [{}]", processId, returnValue);
            getJdbcTemplate().update("UPDATE TSK_PROCESS SET END_TIME = ?, STATE = ?, RETURN_VALUE= ? WHERE PROCESS_ID = ?",
                    new Date().getTime(), Process.FINISHED,
                    PgQueryUtils.asJsonbString(returnValue), processId.toString());
        } catch (Throwable ex) {
            String message = "DB exception on finish process id["+processId+"], return value["+returnValue+"]";
            logger.error(message, ex);
            throw new ServiceCriticalException(message, ex);
        }
    }

    @Override
    public void deleteProcess(UUID processId) {
        try {
            getJdbcTemplate().update("DELETE FROM TSK_PROCESS WHERE PROCESS_ID = ?", processId.toString());
        } catch (Throwable ex) {
            String message = "DB exception on deleting process id["+processId+"]";
            logger.error(message, ex);
            throw new ServiceCriticalException(message, ex);
        }
    }

    @Override
    public TaskContainer getStartTask(UUID processId) {
        try {
            return getJdbcTemplate().queryForObject("SELECT START_JSON FROM TSK_PROCESS WHERE PROCESS_ID = ? ",
                    (rs, rowNum) -> PgQueryUtils.readValue(rs.getString("start_json"), TaskContainer.class), processId.toString());
        } catch (EmptyResultDataAccessException ex) {
            return null;
        } catch (Throwable ex) {
            String message = "DB exception on getStartTask for process id ["+processId+"]";
            logger.error(message, ex);
            throw new ServiceCriticalException(message, ex);
        }
    }

    @Override
    public void markProcessAsBroken(UUID processId) {
        setProcessState(processId, Process.BROKEN);
    }

    @Override
    public void markProcessAsStarted(UUID processId) {
        setProcessState(processId, Process.ACTIVE);
    }

    @Override
    public void markProcessAsAborted(UUID processId) {
        setProcessState(processId, Process.ABORTED);
    }

    private void setProcessState(UUID processId, int state) {
        try {
            getJdbcTemplate().update("UPDATE TSK_PROCESS SET STATE = ? WHERE PROCESS_ID = ?", state, processId.toString());
        } catch (Throwable ex) {
            String message = "DB exception on set state["+state+"] for process["+processId+"]";
            logger.error(message, ex);
            throw new ServiceCriticalException(message, ex);
        }
    }

    @Override
    public Process getProcess(UUID processUUID) {
        try {
            return getJdbcTemplate().queryForObject("SELECT PROCESS_ID, START_TASK_ID, CUSTOM_ID, START_TIME, END_TIME, STATE, RETURN_VALUE, START_JSON " +
                    "FROM TSK_PROCESS WHERE PROCESS_ID = ?", processMapper, processUUID.toString());
        } catch (EmptyResultDataAccessException ex) {
            return null;
        } catch (Throwable ex) {
            String message = "DB exception on getProcess by uuid["+processUUID+"]";
            logger.error(message, ex);
            throw new ServiceCriticalException(message, ex);
        }
    }

    @Override
    public ResultSetCursor<UUID> findIncompleteProcesses(long recoveryTime, int limit) {
        return new UuidResultSetCursor(getDataSource(), "process_id", limit) {

            @Override
            protected PreparedStatement prepareStatement(Connection conn) throws SQLException {
                PreparedStatement ps = connection.prepareStatement(SQL_FIND_INCOMPLETE_PROCESSES);
                ps.setInt(1, Process.ACTIVE);
                ps.setLong(2, recoveryTime);
                ps.setFetchSize(limit);
                return ps;
            }

            @Override
            protected String constructErrorMessage(Throwable e) {
                return "Error finding incomplete processes before time[" + recoveryTime + "], limit[" + limit + "]";
            }
        };
    }

    @Override
    public ResultSetCursor<UUID> findLostProcesses(long lastFinishedProcessDeleteTime, long lastAbortedProcessDeleteTime, int batchSize) {
        return new UuidResultSetCursor(getDataSource(), "process_id", batchSize) {

            @Override
            protected PreparedStatement prepareStatement(Connection conn) throws SQLException {
                PreparedStatement ps = connection.prepareStatement(SQL_FIND_LOST_PROCESSES);
                ps.setInt(1, Process.ABORTED);
                ps.setLong(2, lastAbortedProcessDeleteTime);
                ps.setInt(3, Process.FINISHED);
                ps.setLong(4, lastFinishedProcessDeleteTime);
                ps.setFetchSize(batchSize);
                return ps;
            }

            @Override
            protected String constructErrorMessage(Throwable e) {
                return "Error finding lost processes before time[" + lastFinishedProcessDeleteTime + "], batchSize[" + batchSize + "]";
            }
        };
    }

    @Override
    public GenericPage<Process> findProcesses(ProcessSearchCommand command) {
        StringBuilder queryBuilder = new StringBuilder(SQL_GET_PROCESS_LIST);
        StringBuilder countBuilder = new StringBuilder(SQL_GET_PROCESS_LIST_CNT);
        List<Object> params = new ArrayList<>();
        List<Object> cntParams = new ArrayList<>();
        appendFilterConditions(queryBuilder, params, command);
        appendFilterConditions(countBuilder, cntParams, command);

        queryBuilder.append(" ORDER BY start_time desc ");
        appendPaginationConditions(queryBuilder, params, command);

        long totalCnt = getJdbcTemplate().queryForObject(countBuilder.toString(), Long.class, cntParams.toArray());
        List<Process> items = getJdbcTemplate().query(queryBuilder.toString(), processMapper, params.toArray());

        logger.debug("Find processes called with command [{}], params[{}] and query [{}]", command, params, queryBuilder.toString());

        return new GenericPage<>(items, command.getPageNum(), command.getPageSize(), totalCnt);
    }

    static void appendPaginationConditions(StringBuilder sql, List<Object> params, ProcessSearchCommand command) {
        int offset = (command.getPageNum() - 1) * command.getPageSize();
        int limit = command.getPageSize();
        if (offset > 0) {
            sql.append(" OFFSET ? ");
            params.add(offset);
        }

        sql.append(" LIMIT ? ");
        params.add(limit);
    }

    static void appendFilterConditions(StringBuilder sql, List<Object> params, ProcessSearchCommand command) {
        boolean first = true;
        if (command.getActorId() != null) {
            sql.append(first?" WHERE ":" AND ");
            sql.append("actor_id like ?");
            params.add(command.getActorId().trim() + WILDCARD);
            first = false;
        }
        if (command.getProcessId() != null) {
            sql.append(first?" WHERE ":" AND ");
            sql.append("process_id like ?");
            params.add(command.getProcessId().trim() + WILDCARD);
            first = false;
        }
        if (command.getCustomId() != null) {
            sql.append(first?" WHERE ":" AND ");
            sql.append("custom_id like ?");
            params.add(command.getCustomId().trim() + WILDCARD);
            first = false;
        }
        if (command.getState() >= 0) {
            sql.append(first?" WHERE ":" AND ");
            sql.append("state = ?");
            params.add(command.getState());
            first = false;
        }
        if (command.getStartedFrom() > 0) {
            sql.append(first?" WHERE ":" AND ");
            sql.append("START_TIME >= ?");
            params.add(command.getStartedFrom());
            first = false;
        }
        if (command.getStartedTill() > 0) {
            sql.append(first?" WHERE ":" AND ");
            sql.append("START_TIME <= ?");
            params.add(command.getStartedTill());
            first = false;
        }

    }

    @Override
    public int getFinishedCount(String customId) {
        try {
            return customId != null
                    ? getJdbcTemplate().queryForObject(SQL_GET_PROCESS_CNT_BY_STATE + " AND CUSTOM_ID = ? ", Integer.class, Process.FINISHED, customId)
                    : getJdbcTemplate().queryForObject(SQL_GET_PROCESS_CNT_BY_STATE, Integer.class, Process.FINISHED);
        } catch (Throwable e) {
            String message = "DB error counting finished processes";
            logger.error(message, e);
            throw new ServiceCriticalException(message, e);
        }
    }

    @Override
    public int getBrokenProcessCount() {
        try {
            return getJdbcTemplate().queryForObject(SQL_GET_PROCESS_CNT_BY_STATE, Integer.class, Process.BROKEN);
        } catch (Throwable e) {
            String message = "DB error counting broken processes";
            logger.error(message, e);
            throw new ServiceCriticalException(message, e);
        }
    }

    @Override
    public int getActiveCount(String actorId, String taskList) {
        try {
            return taskList != null
                    ? getJdbcTemplate().queryForObject(SQL_GET_PROCESS_CNT_BY_STATE_AND_STARTER_ID + " AND TASK_LIST = ?", Integer.class, Process.ACTIVE, actorId, taskList)
                    : getJdbcTemplate().queryForObject(SQL_GET_PROCESS_CNT_BY_STATE_AND_STARTER_ID + " AND TASK_LIST is NULL", Integer.class, Process.ACTIVE, actorId);

        } catch (Throwable e) {
            String message = "DB error counting active processes for actorId["+actorId+"], taskList["+taskList+"]";
            logger.error(message, e);
            throw new ServiceCriticalException(message, e);
        }
    }

}
