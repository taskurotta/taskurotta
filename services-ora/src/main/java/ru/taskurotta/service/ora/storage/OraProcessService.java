package ru.taskurotta.service.ora.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.exception.ServiceCriticalException;
import ru.taskurotta.service.common.ResultSetCursor;
import ru.taskurotta.service.console.model.GenericPage;
import ru.taskurotta.service.console.model.Process;
import ru.taskurotta.service.console.retriever.ProcessInfoRetriever;
import ru.taskurotta.service.console.retriever.command.ProcessSearchCommand;
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
public class OraProcessService implements ProcessService, ProcessInfoRetriever {

    private DataSource dataSource;

    private final static Logger logger = LoggerFactory.getLogger(OraProcessService.class);

    private JsonSerializer<TaskContainer> taskSerializer = new JsonSerializer<>(TaskContainer.class);

    protected static final String SQL_GET_PROCESS_CNT_BY_STATE =
            "SELECT COUNT(PROCESS_ID) AS cnt FROM TSK_PROCESS WHERE STATE = ?";
    protected static final String SQL_FIND_INCOMPLETE_PROCESSES =
            "SELECT PROCESS_ID FROM TSK_PROCESS WHERE STATE = ? AND START_TIME < ?";
    protected static final String SQL_GET_PROCESS_LIST =
            "SELECT PROCESS_ID, START_TASK_ID, CUSTOM_ID, START_TIME, END_TIME, STATE, RETURN_VALUE, START_JSON FROM TSK_PROCESS";

    public OraProcessService(DataSource dataSource) {
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
    public GenericPage<Process> listProcesses(int pageNumber, int pageSize, int status, String typeFilter) {
        StringBuilder stringBuilder = new StringBuilder(SQL_GET_PROCESS_LIST);
        if (status >= 0 || typeFilter != null) {
            stringBuilder.append(" WHERE");
            boolean anotherConditionExists = false;
            if (status >= 0) {
                stringBuilder.append(" STATE = ?");
                anotherConditionExists = true;
            }
            if (typeFilter != null) {
                if (anotherConditionExists) {
                    stringBuilder.append(" AND");
                }
                stringBuilder.append(" ACTOR_ID LIKE ?");
            }
        }
        String query = stringBuilder.toString();

        List<Process> result = new ArrayList<>();
        long totalCount = 0;
        ResultSet rs = null;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(createPagesQuery(query))) {

            int argIndex = 1;
            if (status >= 0) {
                ps.setInt(argIndex++, status);
            }

            if (typeFilter != null) {
                ps.setString(argIndex++, typeFilter + "%");
            }

            int startIndex = (pageNumber - 1) * pageSize + 1;
            int endIndex = startIndex + pageSize - 1;
            ps.setInt(argIndex++, endIndex);
            ps.setInt(argIndex, startIndex);

            rs = ps.executeQuery();
            while (rs.next()) {
                result.add(createProcessFromResultSet(rs));
                totalCount = rs.getLong("cnt");
            }
        } catch (SQLException ex) {
            logger.error("DataBase exception: " + ex.getMessage(), ex);
            throw new ServiceCriticalException("Database error", ex);
        } finally {
            closeResultSet(rs);
        }

        logger.debug("Process list got by params: pageNum[{}], pageSize[{}], status[{}] is [{}]", pageNumber, pageSize, status, result);

        return new GenericPage<>(result, pageNumber, pageSize, totalCount);
    }

    @Override
    public List<Process> findProcesses(ProcessSearchCommand command) {
        List<Process> result = new ArrayList<>();
        if (command != null && !command.isEmpty()) {
            String query = getSearchSql(command);
            ResultSet rs = null;
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement ps = connection.prepareStatement(query)
            ) {
                rs = ps.executeQuery();
                while (rs.next()) {
                    result.add(createProcessFromResultSet(rs));
                }
            } catch (SQLException ex) {
                logger.error("DataBase exception on query [" + query + "]: " + ex.getMessage(), ex);
                throw new ServiceCriticalException("Database error", ex);
            } finally {
                closeResultSet(rs);
            }
        }
        return result;
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

    private static String getSearchSql(ProcessSearchCommand command) {
        StringBuilder sb = new StringBuilder("SELECT PROCESS_ID, START_TASK_ID, CUSTOM_ID, START_TIME, END_TIME, STATE, START_JSON, RETURN_VALUE FROM TSK_PROCESS WHERE ");
        boolean requireAndCdtn = false;
        if (command.getProcessId() != null && command.getProcessId().trim().length() > 0) {
            sb.append("PROCESS_ID LIKE '").append(command.getProcessId()).append("%'");
            requireAndCdtn = true;
        }
        if (command.getCustomId() != null && command.getCustomId().trim().length() > 0) {
            if (requireAndCdtn) {
                sb.append(" AND ");
            }
            sb.append("CUSTOM_ID LIKE '").append(command.getCustomId()).append("%'");
        }

        sb.append(" AND ROWNUM <= 200");

        return sb.toString();
    }
}
