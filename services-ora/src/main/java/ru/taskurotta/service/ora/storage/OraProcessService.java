package ru.taskurotta.service.ora.storage;

import com.hazelcast.core.HazelcastInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.exception.ServiceCriticalException;
import ru.taskurotta.service.common.ResultSetCursor;
import ru.taskurotta.service.console.model.GenericPage;
import ru.taskurotta.service.console.model.Process;
import ru.taskurotta.service.console.retriever.ProcessInfoRetriever;
import ru.taskurotta.service.console.retriever.command.ProcessSearchCommand;
import ru.taskurotta.service.hz.storage.AbstractHzProcessService;
import ru.taskurotta.service.storage.ProcessService;
import ru.taskurotta.transport.model.TaskContainer;
import ru.taskurotta.transport.model.serialization.JsonSerializer;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * User: moroz
 * Date: 25.04.13
 */
public class OraProcessService extends AbstractHzProcessService implements ProcessService, ProcessInfoRetriever {

    private DataSource dataSource;

    private final static Logger logger = LoggerFactory.getLogger(OraProcessService.class);

    private JsonSerializer<TaskContainer> taskSerializer = new JsonSerializer<>(TaskContainer.class);

    protected static final String SQL_GET_PROCESS_CNT_BY_STATE =
            "SELECT COUNT(PROCESS_ID) AS cnt FROM TSK_PROCESS WHERE STATE = ?";
    protected static final String SQL_FIND_INCOMPLETE_PROCESSES =
            "SELECT PROCESS_ID FROM TSK_PROCESS WHERE STATE = ? AND START_TIME < ?";
    protected static final String SQL_GET_PROCESS_LIST =
            "SELECT PROCESS_ID, START_TASK_ID, CUSTOM_ID, START_TIME, END_TIME, STATE, RETURN_VALUE, START_JSON FROM TSK_PROCESS";
    protected static final String SQL_GET_ORDERED_PROCESS_LIST =
            "SELECT /*+ INDEX_ASC(TSK_PROCESS TSK_PROCESS_START_TIME_IDX) */ PROCESS_ID, START_TASK_ID, CUSTOM_ID, START_TIME, END_TIME, STATE, RETURN_VALUE, START_JSON FROM TSK_PROCESS";

    public static final String WILDCARD = "%";

    public OraProcessService(HazelcastInstance hzInstance, DataSource dataSource) {
        super(hzInstance);
        this.dataSource = dataSource;
    }

    @Override
    public void finishProcess(UUID processId, String returnValue) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(
                     "UPDATE TSK_PROCESS SET END_TIME = ?, STATE = ?, RETURN_VALUE= ? WHERE PROCESS_ID = ?")
        ) {
            ps.setLong(1, (new Date()).getTime());
            ps.setInt(2, Process.FINISH);
            ps.setString(3, returnValue);
            ps.setString(4, processId.toString());
            ps.executeUpdate();
        } catch (SQLException ex) {
            logger.error("DataBase exception: " + ex.getMessage(), ex);
            throw new ServiceCriticalException("Database error", ex);
        }
    }

    @Override
    public void deleteProcess(UUID processId) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement("DELETE FROM TSK_PROCESS WHERE PROCESS_ID = ?")
        ) {
            ps.setString(1, processId.toString());
            ps.executeUpdate();
        } catch (SQLException ex) {
            logger.error("DataBase exception: " + ex.getMessage(), ex);
            throw new ServiceCriticalException("Database error", ex);
        }
    }

    @Override
    public void startProcess(TaskContainer task) {

        logger.debug("Starting process with TaskContainer [{}]", task);

        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(
                     "INSERT INTO TSK_PROCESS (PROCESS_ID, START_TASK_ID, CUSTOM_ID, START_TIME, STATE, START_JSON, ACTOR_ID) " +
                             "VALUES (?, ?, ?, ?, ?, ?, ?)")
        ) {
            ps.setString(1, task.getProcessId().toString());
            ps.setString(2, task.getTaskId().toString());
            ps.setString(3, ((task.getOptions() != null) && (task.getOptions().getTaskConfigContainer() != null)) ?
                    task.getOptions().getTaskConfigContainer().getCustomId() : null);
            ps.setLong(4, (new Date()).getTime());
            ps.setInt(5, Process.START);
            ps.setString(6, (String) taskSerializer.serialize(task));
            ps.setString(7, task.getActorId());
            ps.executeUpdate();
        } catch (SQLException ex) {
            logger.error("DataBase exception: " + ex.getMessage(), ex);
            throw new ServiceCriticalException("Database error", ex);
        }
    }

    @Override
    public Process getProcess(UUID processUUID) {
        Process result = null;
        ResultSet rs = null;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(
                     "SELECT PROCESS_ID, START_TASK_ID, CUSTOM_ID, START_TIME, END_TIME, STATE, RETURN_VALUE, START_JSON " +
                             "FROM TSK_PROCESS WHERE PROCESS_ID = ?")
        ) {
            ps.setString(1, processUUID.toString());
            rs = ps.executeQuery();
            while (rs.next()) {
                result = createProcessFromResultSet(rs);
            }
        } catch (SQLException ex) {
            logger.error("DataBase exception: " + ex.getMessage(), ex);
            throw new ServiceCriticalException("Database error", ex);
        } finally {
            closeResultSet(rs);
        }
        return result;
    }

    @Override
    public TaskContainer getStartTask(UUID processId) {
        TaskContainer result = null;
        ResultSet rs = null;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement("SELECT START_JSON FROM TSK_PROCESS WHERE PROCESS_ID = ?")
        ) {
            ps.setString(1, processId.toString());
            rs = ps.executeQuery();
            while (rs.next()) {
                result = taskSerializer.deserialize(rs.getString("start_json"));
            }
        } catch (SQLException ex) {
            logger.error("DataBase exception: " + ex.getMessage(), ex);
            throw new ServiceCriticalException("Database error", ex);
        } finally {
            closeResultSet(rs);
        }
        return result;
    }

    @Override
    public void markProcessAsBroken(UUID processId) {
        setProcessState(processId, Process.BROKEN);
    }

    @Override
    public void markProcessAsStarted(UUID processId) {
        setProcessState(processId, Process.START);
    }

    @Override
    public void markProcessAsAborted(UUID processId) {
        setProcessState(processId, Process.ABORTED);
    }

    private void setProcessState(UUID processId, int state) {
        logger.trace("Try to mark process [{}] state as ([{}])", processId, state);

        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement("UPDATE TSK_PROCESS SET STATE = ? WHERE PROCESS_ID = ?")) {

            ps.setInt(1, state);
            ps.setString(2, processId.toString());
            ps.executeUpdate();
        } catch (SQLException ex) {
            logger.error("DataBase exception: " + ex.getMessage(), ex);
            throw new ServiceCriticalException("Database error", ex);
        }

        logger.debug("Process [{}] state marked as ([{}])", processId, state);
    }

    @Override
    public GenericPage<Process> findProcesses(ProcessSearchCommand command) {
        StringBuilder queryBuilder = new StringBuilder();
        if (command.isFilterEmpty()) {
            queryBuilder.append(SQL_GET_ORDERED_PROCESS_LIST);
        } else {
            queryBuilder.append(SQL_GET_PROCESS_LIST);
        }
        List<Object> params = new ArrayList<>();
        appendFilterConditions(queryBuilder, params, command);

        String query = createPagesQuery(queryBuilder.toString());
        int startIndex = (command.getPageNum() - 1) * command.getPageSize() + 1;
        int endIndex = startIndex + command.getPageSize() - 1;
        params.add(endIndex);
        params.add(startIndex);

        logger.debug("Find processes called with command [{}], params[{}] and query [{}]", command, params, query);

        List<Process> items = new ArrayList<>();
        long totalCnt = 0l;
        ResultSet rs = null;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(query)
        ) {
            appendFilterConditions(ps, params);
            rs = ps.executeQuery();
            while (rs.next()) {
                items.add(createProcessFromResultSet(rs));
                totalCnt = rs.getLong("cnt");
            }
        } catch (SQLException ex) {
            logger.error("DataBase exception on query [" + query + "]: " + ex.getMessage(), ex);
            throw new ServiceCriticalException("Database error", ex);
        } finally {
            closeResultSet(rs);
        }
        return new GenericPage<>(items, command.getPageNum(), command.getPageSize(), totalCnt);
    }

    static void appendFilterConditions(PreparedStatement ps, List<Object> params) throws SQLException {
        if (params!=null && !params.isEmpty()) {
            int pos = 1;
            for (Object param : params) {
                if (param instanceof String) {
                    ps.setString(pos, (String) param);
                } else if (param instanceof Long) {
                    ps.setLong(pos, (Long) param);
                } else if (param instanceof Integer) {
                    ps.setInt(pos, (Integer) param);
                } else {
                    ps.setObject(pos, param);
                }
                pos++;
            }
        }
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
        int result = 0;
        String sql = customId != null ? SQL_GET_PROCESS_CNT_BY_STATE + " AND CUSTOM_ID = ? " : SQL_GET_PROCESS_CNT_BY_STATE;
        ResultSet resultSet = null;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setInt(1, Process.FINISH);
            if (customId != null) {
                ps.setString(2, customId);
            }
            resultSet = ps.executeQuery();
            while (resultSet.next()) {
                result = resultSet.getInt("cnt");
            }
        } catch (SQLException ex) {
            logger.error("DataBase exception: " + ex.getMessage(), ex);
            throw new ServiceCriticalException("Database error", ex);
        } finally {
            closeResultSet(resultSet);
        }

        return result;
    }

    private Process createProcessFromResultSet(ResultSet resultSet) throws SQLException {
        Process process = new Process();

        process.setProcessId(UUID.fromString(resultSet.getString("process_id")));
        process.setStartTaskId(UUID.fromString(resultSet.getString("start_task_id")));
        process.setCustomId(resultSet.getString("custom_id"));
        process.setStartTime(resultSet.getLong("start_time"));
        process.setEndTime(resultSet.getLong("end_time"));
        process.setState(resultSet.getInt("state"));
        process.setReturnValue(resultSet.getString("return_value"));
        process.setStartTask(taskSerializer.deserialize(resultSet.getString("start_json")));

        return process;
    }

    @Override
    public ResultSetCursor<UUID> findIncompleteProcesses(final long recoveryTime, final int limit) {
        return new ResultSetCursor<UUID>() {

            private Connection connection;
            private PreparedStatement preparedStatement;
            private ResultSet resultSet;
            private int counter;

            private void init() throws SQLException {
                connection = dataSource.getConnection();
                preparedStatement = connection.prepareStatement(SQL_FIND_INCOMPLETE_PROCESSES);

                preparedStatement.setInt(1, Process.START);
                preparedStatement.setLong(2, recoveryTime);
                preparedStatement.setFetchSize(limit);

                resultSet = preparedStatement.executeQuery();
            }

            @Override
            public Collection<UUID> getNext() {
                Collection<UUID> result = new ArrayList<>(limit);
                try {
                    if (resultSet == null) {
                        init();
                    }

                    while (counter < limit && resultSet.next()) {
                        result.add(UUID.fromString(resultSet.getString("process_id")));
                        counter++;
                    }
                    counter = 0;
                } catch (SQLException e) {
                    logger.error("Catch exception when finding incomplete processes before time[" + recoveryTime + "] processes limit[" + limit + "]", e);
                    throw new ServiceCriticalException("Incomplete processes search before time[" + recoveryTime + "] failed", e);
                }

                return result;
            }

            @Override
            public void close() throws IOException {
                closeResultSet(resultSet);

                if (preparedStatement != null) {
                    try {
                        preparedStatement.close();
                    } catch (SQLException e) {
                        throw new ServiceCriticalException("Error on closing PreparedStatement", e);
                    }
                }

                if (connection != null) {
                    try {
                        connection.close();
                    } catch (SQLException e) {
                        throw new ServiceCriticalException("Error on closing Connection", e);
                    }
                }
            }
        };
    }

    @Override
    public int getBrokenProcessCount() {
        int result = 0;
        ResultSet resultSet = null;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(SQL_GET_PROCESS_CNT_BY_STATE)) {
            ps.setInt(1, Process.BROKEN);
            resultSet = ps.executeQuery();
            while (resultSet.next()) {
                result = resultSet.getInt("cnt");
            }
        } catch (SQLException ex) {
            logger.error("DataBase exception: " + ex.getMessage(), ex);
            throw new ServiceCriticalException("Database error", ex);
        } finally {
            closeResultSet(resultSet);
        }
        return result;
    }

    private static String createPagesQuery(String query) {
        return "SELECT t1.* FROM ( SELECT t.*, ROWNUM rnum FROM ( select a1.*, count(*) over() as cnt FROM ( " +
                query +
                " ) a1) t WHERE ROWNUM <= ? ) t1 WHERE t1.rnum >= ?";
    }

    private static void closeResultSet(ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
                throw new ServiceCriticalException("Error on closing ResultSet", e);
            }
        }
    }

}
