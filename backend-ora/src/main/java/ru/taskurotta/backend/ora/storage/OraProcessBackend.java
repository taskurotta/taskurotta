package ru.taskurotta.backend.ora.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.backend.console.model.GenericPage;
import ru.taskurotta.backend.console.model.ProcessVO;
import ru.taskurotta.backend.console.retriever.ProcessInfoRetriever;
import ru.taskurotta.backend.console.retriever.command.ProcessSearchCommand;
import ru.taskurotta.backend.ora.tools.PagedQueryBuilder;
import ru.taskurotta.backend.storage.ProcessBackend;
import ru.taskurotta.exception.BackendCriticalException;
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
public class OraProcessBackend implements ProcessBackend, ProcessInfoRetriever {

    private DataSource dataSource;

    private final static Logger log = LoggerFactory.getLogger(OraProcessBackend.class);

    private JsonSerializer<TaskContainer> taskSerializer = new JsonSerializer<>(TaskContainer.class);

    public OraProcessBackend(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void finishProcess(UUID processId, String returnValue) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement("UPDATE PROCESS SET end_time = ?, state = ?, return_value= ? WHERE process_id = ?")
        ) {
            ps.setLong(1, (new Date()).getTime());
            ps.setInt(2, 1);
            ps.setString(3, returnValue);
            ps.setString(4, processId.toString());
            ps.executeUpdate();
        } catch (SQLException ex) {
            log.error("DataBase exception: " + ex.getMessage(), ex);
            throw new BackendCriticalException("Database error", ex);
        }
    }

    @Override
    public void startProcess(TaskContainer task) {

        log.debug("Starting process with TaskContainer [{}]", task);

        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement("INSERT INTO PROCESS (process_id, start_task_id, custom_id, start_time, state, start_json) VALUES (?, ?, ?, ?, ?,?)")
        ) {
            ps.setString(1, task.getProcessId().toString());
            ps.setString(2, task.getTaskId().toString());
            ps.setString(3, ((task.getOptions() != null) && (task.getOptions().getActorSchedulingOptions() != null)) ?
                    task.getOptions().getActorSchedulingOptions().getCustomId() : null);
            ps.setLong(4, (new Date()).getTime());
            ps.setInt(5, 0);
            ps.setString(6, (String) taskSerializer.serialize(task));
            ps.executeUpdate();
        } catch (SQLException ex) {
            log.error("DataBase exception: " + ex.getMessage(), ex);
            throw new BackendCriticalException("Database error", ex);
        }
    }

    @Override
    public ProcessVO getProcess(UUID processUUID) {
        ProcessVO result = null;
        ResultSet rs = null;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement("SELECT PROCESS_ID, START_TASK_ID, CUSTOM_ID, START_TIME, END_TIME, STATE, RETURN_VALUE FROM PROCESS WHERE PROCESS_ID = ?")
        ) {
            ps.setString(1, processUUID.toString());
            rs = ps.executeQuery();
            while (rs.next()) {
                result = new ProcessVO();
                result.setProcessUuid(processUUID);
                result.setStartTaskUuid(UUID.fromString(rs.getString("start_task_id")));
                result.setCustomId(rs.getString("custom_id"));
                result.setStartTime(rs.getLong("start_time"));
                result.setEndTime(rs.getLong("end_time"));
                result.setReturnValueJson(rs.getString("return_value"));
            }
        } catch (SQLException ex) {
            log.error("DataBase exception: " + ex.getMessage(), ex);
            throw new BackendCriticalException("Database error", ex);
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
            throw new BackendCriticalException("Database error", ex);
        } finally {
            closeResultSet(rs);
        }
        return result;
    }

    @Override
    public GenericPage<ProcessVO> listProcesses(int pageNumber, int pageSize) {
        List<ProcessVO> result = null;
        long totalCount = 0;
        ResultSet rs = null;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(PagedQueryBuilder.createPagesQuery("SELECT PROCESS_ID, START_TASK_ID, CUSTOM_ID, START_TIME, END_TIME, STATE, RETURN_VALUE FROM PROCESS"))
        ) {
            int startIndex = (pageNumber - 1) * pageSize + 1;
            int endIndex = startIndex + pageSize - 1;
            ps.setInt(1, endIndex);
            ps.setInt(2, startIndex);

            rs = ps.executeQuery();
            while (rs.next()) {
                if (result == null) {
                    result = new ArrayList<>();
                }
                ProcessVO process = new ProcessVO();
                process.setProcessUuid(UUID.fromString(rs.getString("process_id")));
                process.setStartTaskUuid(UUID.fromString(rs.getString("start_task_id")));
                process.setCustomId(rs.getString("custom_id"));
                process.setStartTime(rs.getLong("start_time"));
                process.setEndTime(rs.getLong("end_time"));
                process.setReturnValueJson(rs.getString("return_value"));
                result.add(process);
                totalCount = rs.getLong("cnt");
            }
        } catch (SQLException ex) {
            log.error("DataBase exception: " + ex.getMessage(), ex);
            throw new BackendCriticalException("Database error", ex);
        } finally {
            closeResultSet(rs);
        }

        return new GenericPage<>(result, pageNumber, pageSize, totalCount);
    }

    private static void closeResultSet(ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
                throw new BackendCriticalException("Error on closing ResultSet", e);
            }
        }
    }

    @Override
    public List<ProcessVO> findProcesses(ProcessSearchCommand command) {
        List<ProcessVO> result = new ArrayList<>();
        if(command!=null && !command.isEmpty()) {
            String query = getSearchSql(command);
            ResultSet rs = null;
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement ps = connection.prepareStatement(query)
            ) {
                rs = ps.executeQuery();
                while (rs.next()) {
                    ProcessVO process = new ProcessVO();
                    process.setProcessUuid(UUID.fromString(rs.getString("process_id")));
                    process.setStartTaskUuid(UUID.fromString(rs.getString("start_task_id")));
                    process.setCustomId(rs.getString("custom_id"));
                    process.setStartTime(rs.getLong("start_time"));
                    process.setEndTime(rs.getLong("end_time"));
                    process.setReturnValueJson(rs.getString("return_value"));
                    result.add(process);
                }
            } catch (SQLException ex) {
                log.error("DataBase exception on query ["+query+"]: " + ex.getMessage(), ex);
                throw new BackendCriticalException("Database error", ex);
            } finally {
                closeResultSet(rs);
            }
        }
        return result;
    }

    private String getSearchSql(ProcessSearchCommand command) {
        StringBuilder sb = new StringBuilder("SELECT PROCESS_ID, START_TASK_ID, CUSTOM_ID, START_TIME, END_TIME, STATE, RETURN_VALUE FROM PROCESS WHERE ");
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

}
