package ru.taskurotta.backend.ora.storage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.backend.checkpoint.CheckpointService;
import ru.taskurotta.backend.checkpoint.TimeoutType;
import ru.taskurotta.backend.checkpoint.model.Checkpoint;
import ru.taskurotta.backend.console.model.GenericPage;
import ru.taskurotta.backend.console.model.ProcessVO;
import ru.taskurotta.backend.console.retriever.ProcessInfoRetriever;
import ru.taskurotta.backend.dependency.model.DependencyDecision;
import ru.taskurotta.backend.ora.tools.PagedQueryBuilder;
import ru.taskurotta.backend.storage.ProcessBackend;
import ru.taskurotta.exception.BackendCriticalException;
import ru.taskurotta.transport.model.DecisionContainer;
import ru.taskurotta.transport.model.TaskContainer;

/**
 * User: moroz
 * Date: 25.04.13
 */
public class OraProcessBackend implements ProcessBackend, ProcessInfoRetriever {

    private CheckpointService checkpointService;

    private DataSource dataSource;

    private final static Logger log = LoggerFactory.getLogger(OraProcessBackend.class);

    public OraProcessBackend(DataSource dataSource, CheckpointService checkpointService) {
        this.dataSource = dataSource;
        this.checkpointService = checkpointService;
    }

    @Override
    public void finishProcess(DependencyDecision dependencyDecision, String returnValue) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement("UPDATE PROCESS SET end_time = ?, state = ?, return_value= ? WHERE process_id = ?")
        ) {
            ps.setLong(1, (new Date()).getTime());
            ps.setInt(2, 1);
            ps.setString(3, returnValue);
            ps.setString(4, dependencyDecision.getFinishedProcessId().toString());
            ps.executeUpdate();
            checkpointService.removeEntityCheckpoints(dependencyDecision.getFinishedProcessId(), TimeoutType.PROCESS_START_TO_CLOSE);
            checkpointService.removeEntityCheckpoints(dependencyDecision.getFinishedProcessId(), TimeoutType.PROCESS_SCHEDULE_TO_CLOSE);
        } catch (SQLException ex) {
            log.error("DataBase exception: " + ex.getMessage(), ex);
            throw new BackendCriticalException("Database error", ex);
        }


    }

    @Override
    public void startProcess(TaskContainer task) {
        checkpointService.addCheckpoint(new Checkpoint(TimeoutType.PROCESS_SCHEDULE_TO_CLOSE, task.getProcessId(), task.getActorId(), task.getStartTime()));
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement("INSERT INTO PROCESS (process_id, start_task_id, custom_id, start_time, state) VALUES (?, ?, ?, ?, ?)")
        ) {
            ps.setString(1, task.getProcessId().toString());
            ps.setString(2, task.getTaskId().toString());
            ps.setString(3, ((task.getOptions() != null) && (task.getOptions().getActorSchedulingOptions() != null)) ?
                    task.getOptions().getActorSchedulingOptions().getCustomId() : null);
            ps.setLong(4, (new Date()).getTime());
            ps.setInt(5, 0);
            ps.executeUpdate();
        } catch (SQLException ex) {
            log.error("DataBase exception: " + ex.getMessage(), ex);
            throw new BackendCriticalException("Database error", ex);
        }
    }

    @Override
    public void startProcessCommit(TaskContainer task) {
        checkpointService.addCheckpoint(new Checkpoint(TimeoutType.PROCESS_START_TO_CLOSE, task.getProcessId(), task.getActorId(), task.getStartTime()));
    }

    @Override
    public CheckpointService getCheckpointService() {
        return checkpointService;
    }

    @Override
    public ProcessVO getProcess(UUID processUUID) {
        ProcessVO result = null;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement("SELECT PROCESS_ID, START_TASK_ID, CUSTOM_ID, START_TIME, END_TIME, STATE, RETURN_VALUE FROM PROCESS WHERE PROCESS_ID = ?")
        ) {
            ps.setString(1, processUUID.toString());
            ResultSet rs = ps.executeQuery();
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
        }
        return result;
    }

    @Override
    public GenericPage<ProcessVO> listProcesses(int pageNumber, int pageSize) {
        List<ProcessVO> result = null;
        long totalCount = 0;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(PagedQueryBuilder.createPagesQuery("SELECT PROCESS_ID, START_TASK_ID, CUSTOM_ID, START_TIME, END_TIME, STATE, RETURN_VALUE FROM PROCESS"));
        ) {
            int startIndex = (pageNumber - 1) * pageSize + 1;
            int endIndex = startIndex + pageSize - 1;
            ps.setInt(1, endIndex);
            ps.setInt(2, startIndex);

            ResultSet rs = ps.executeQuery();
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
        }

        return new GenericPage(result, pageNumber, pageSize, totalCount);
    }

    @Override
    public List<ProcessVO> findProcesses(String type, String id) {
        List<ProcessVO> result = new ArrayList<>();
        String query = "SELECT PROCESS_ID, START_TASK_ID, CUSTOM_ID, START_TIME, END_TIME, STATE, RETURN_VALUE FROM PROCESS WHERE %ST LIKE ? AND ROWNUM <= 200";
        query = (!SEARCH_BY_CUSTOM_ID.equals(type)) ? query.replace("%ST", "PROCESS_ID") : query.replace("%ST", "CUSTOM_ID");
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(query);
        ) {
            if ((id != null) && (!id.isEmpty())) {
                ps.setString(1, id + "%");
                ResultSet rs = ps.executeQuery();
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
            }
        } catch (SQLException ex) {
            log.error("DataBase exception: " + ex.getMessage(), ex);
            throw new BackendCriticalException("Database error", ex);
        }
        return result;
    }
}
