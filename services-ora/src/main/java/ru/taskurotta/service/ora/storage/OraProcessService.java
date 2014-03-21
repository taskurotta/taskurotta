package ru.taskurotta.service.ora.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.exception.ServiceCriticalException;
import ru.taskurotta.service.console.model.GenericPage;
import ru.taskurotta.service.console.model.Process;
import ru.taskurotta.service.console.retriever.ProcessInfoRetriever;
import ru.taskurotta.service.console.retriever.command.ProcessSearchCommand;
import ru.taskurotta.service.ora.tools.PagedQueryBuilder;
import ru.taskurotta.service.storage.ProcessService;
import ru.taskurotta.transport.model.TaskContainer;
import ru.taskurotta.transport.model.serialization.JsonSerializer;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * User: moroz
 * Date: 25.04.13
 */
public class OraProcessService implements ProcessService, ProcessInfoRetriever {

    private DataSource dataSource;

    private final static Logger log = LoggerFactory.getLogger(OraProcessService.class);

    private JsonSerializer<TaskContainer> taskSerializer = new JsonSerializer<>(TaskContainer.class);

    protected static final String SQL_GET_PROCESS_CNT_BY_STATE = "SELECT COUNT(PROCESS_ID) AS cnt FROM PROCESS WHERE STATE = ? ";

    public OraProcessService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void finishProcess(UUID processId, String returnValue) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement("UPDATE PROCESS SET end_time = ?, state = ?, return_value= ? WHERE process_id = ?")
        ) {
            ps.setLong(1, (new Date()).getTime());
            ps.setInt(2, Process.FINISH);
            ps.setString(3, returnValue);
            ps.setString(4, processId.toString());
            ps.executeUpdate();
        } catch (SQLException ex) {
            log.error("DataBase exception: " + ex.getMessage(), ex);
            throw new ServiceCriticalException("Database error", ex);
        }
    }

    @Override
    public void deleteProcess(UUID processId) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement("DELETE FROM PROCESS WHERE process_id = ?")
        ) {
            ps.setString(1, processId.toString());
            ps.executeUpdate();
        } catch (SQLException ex) {
            log.error("DataBase exception: " + ex.getMessage(), ex);
            throw new ServiceCriticalException("Database error", ex);
        }
    }

    @Override
    public void startProcess(TaskContainer task) {

        log.debug("Starting process with TaskContainer [{}]", task);

        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement("INSERT INTO PROCESS (process_id, start_task_id, custom_id, start_time, state, start_json) VALUES (?, ?, ?, ?, ?, ?)")
        ) {
            ps.setString(1, task.getProcessId().toString());
            ps.setString(2, task.getTaskId().toString());
            ps.setString(3, ((task.getOptions() != null) && (task.getOptions().getActorSchedulingOptions() != null)) ?
                    task.getOptions().getActorSchedulingOptions().getCustomId() : null);
            ps.setLong(4, (new Date()).getTime());
            ps.setInt(5, Process.START);
            ps.setString(6, (String) taskSerializer.serialize(task));
            ps.executeUpdate();
        } catch (SQLException ex) {
            log.error("DataBase exception: " + ex.getMessage(), ex);
            throw new ServiceCriticalException("Database error", ex);
        }
    }

    @Override
    public Process getProcess(UUID processUUID) {
        Process result = null;
        ResultSet rs = null;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement("SELECT PROCESS_ID, START_TASK_ID, CUSTOM_ID, START_TIME, END_TIME, STATE, RETURN_VALUE, START_JSON FROM PROCESS WHERE PROCESS_ID = ?")
        ) {
            ps.setString(1, processUUID.toString());
            rs = ps.executeQuery();
            while (rs.next()) {
                result = createProcessFromResultSet(rs);
            }
        } catch (SQLException ex) {
            log.error("DataBase exception: " + ex.getMessage(), ex);
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
             PreparedStatement ps = connection.prepareStatement("SELECT start_json FROM PROCESS WHERE PROCESS_ID = ?")
        ) {
            ps.setString(1, processId.toString());
            rs = ps.executeQuery();
            while (rs.next()) {
                result = taskSerializer.deserialize(rs.getString("start_json"));
            }
        } catch (SQLException ex) {
            log.error("DataBase exception: " + ex.getMessage(), ex);
            throw new ServiceCriticalException("Database error", ex);
        } finally {
            closeResultSet(rs);
        }
        return result;
    }

    @Override
    public void markProcessAsBroken(UUID processId) {
        log.trace("Try to mark process [{}] as broken([{}])", processId, Process.BROKEN);

        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement("UPDATE PROCESS set STATE = ? WHERE PROCESS_ID = ?")) {

            ps.setInt(1, Process.BROKEN);
            ps.setString(2, processId.toString());
            ps.executeUpdate();
        } catch (SQLException ex) {
            log.error("DataBase exception: " + ex.getMessage(), ex);
            throw new ServiceCriticalException("Database error", ex);
        }

        log.debug("Process [{}] marked as broken([{}])", processId, Process.BROKEN);
    }

    @Override
    public GenericPage<Process> listProcesses(int pageNumber, int pageSize, int status) {
        List<Process> result = new ArrayList<>();
        long totalCount = 0;
        ResultSet rs = null;
        String query = "SELECT PROCESS_ID, START_TASK_ID, CUSTOM_ID, START_TIME, END_TIME, STATE, RETURN_VALUE, START_JSON FROM PROCESS ";
        if (status >= 0) {
            query+= "WHERE STATE = ? ";
        }
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(PagedQueryBuilder.createPagesQuery(query))
        ) {
            int argIndex = 1;
            if (status >= 0) {
                ps.setInt(argIndex++, status);
            }
            int startIndex = (pageNumber - 1) * pageSize + 1;
            int endIndex = startIndex + pageSize - 1;
            ps.setInt(argIndex++, endIndex);
            ps.setInt(argIndex++, startIndex);

            rs = ps.executeQuery();
            while (rs.next()) {
                result.add(createProcessFromResultSet(rs));
                totalCount = rs.getLong("cnt");
            }
        } catch (SQLException ex) {
            log.error("DataBase exception: " + ex.getMessage(), ex);
            throw new ServiceCriticalException("Database error", ex);
        } finally {
            closeResultSet(rs);
        }

        log.debug("Process list got by params: pageNum[{}], pageSize[{}], status[{}] is [{}]", pageNumber, pageSize, status, result);

        return new GenericPage<>(result, pageNumber, pageSize, totalCount);
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

    @Override
    public List<Process> findProcesses(ProcessSearchCommand command) {
        List<Process> result = new ArrayList<>();
        if(command!=null && !command.isEmpty()) {
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
                log.error("DataBase exception on query ["+query+"]: " + ex.getMessage(), ex);
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
        String sql = customId!=null? SQL_GET_PROCESS_CNT_BY_STATE + "AND CUSTOM_ID = ? ": SQL_GET_PROCESS_CNT_BY_STATE;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setInt(1, Process.FINISH);
            if (customId != null) {
                ps.setString(2, customId);
            }
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                result = rs.getInt("cnt");
            }
        } catch (SQLException ex) {
            log.error("DataBase exception: " + ex.getMessage(), ex);
            throw new ServiceCriticalException("Database error", ex);
        }
        return result;
    }

    private String getSearchSql(ProcessSearchCommand command) {
        StringBuilder sb = new StringBuilder("SELECT PROCESS_ID, START_TASK_ID, CUSTOM_ID, START_TIME, END_TIME, STATE, START_JSON, RETURN_VALUE FROM PROCESS WHERE ");
        boolean requireAndCdtn = false;
        if(command.getProcessId()!=null && command.getProcessId().trim().length()>0) {
            sb.append("PROCESS_ID LIKE '").append(command.getProcessId()).append("%'");
            requireAndCdtn = true;
        }
        if(command.getCustomId()!=null && command.getCustomId().trim().length()>0) {
            if(requireAndCdtn) {
                sb.append(" AND ");
            }
            sb.append("CUSTOM_ID LIKE '").append(command.getCustomId()).append("%'");
        }

        sb.append(" AND ROWNUM <= 200");

        return sb.toString();
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
}
