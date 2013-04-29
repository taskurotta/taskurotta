package ru.taskurotta.backend.ora.storage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Date;
import java.util.UUID;
import javax.sql.DataSource;

import static ru.taskurotta.backend.ora.tools.SqlResourceCloser.closeResources;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.backend.checkpoint.CheckpointService;
import ru.taskurotta.backend.checkpoint.TimeoutType;
import ru.taskurotta.backend.checkpoint.model.Checkpoint;
import ru.taskurotta.backend.storage.ProcessBackend;
import ru.taskurotta.backend.storage.model.TaskContainer;
import ru.taskurotta.exception.BackendCriticalException;

/**
 * User: moroz
 * Date: 25.04.13
 */
public class OraProcessBackend implements ProcessBackend {

    private CheckpointService checkpointService;

    private DataSource dataSource;

    private final static Logger log = LoggerFactory.getLogger(OraProcessBackend.class);

    public OraProcessBackend(DataSource dataSource, CheckpointService checkpointService) {
        this.dataSource = dataSource;
        this.checkpointService = checkpointService;
    }

    @Override
    public void finishProcess(UUID processId, String returnValue) {

        Connection connection = null;
        PreparedStatement ps = null;
        try {
            connection = dataSource.getConnection();
            ps = connection.prepareStatement("UPDATE PROCESS SET end_time = ?, state = ?, return_value= ? WHERE process_id = ?");
            ps.setLong(1, (new Date()).getTime());
            ps.setInt(2, 1);
            ps.setString(3, returnValue);
            ps.setString(4, processId.toString());
            ps.executeUpdate();
            checkpointService.removeEntityCheckpoints(processId, TimeoutType.PROCESS_START_TO_CLOSE);
            checkpointService.removeEntityCheckpoints(processId, TimeoutType.PROCESS_SCHEDULE_TO_CLOSE);
        } catch (SQLException ex) {
            log.error("DataBase exception: " + ex.getMessage(), ex);
            throw new BackendCriticalException("Database error", ex);
        } finally {
            closeResources(ps, connection);
        }


    }

    @Override
    public void startProcess(TaskContainer task) {
        checkpointService.addCheckpoint(new Checkpoint(TimeoutType.PROCESS_SCHEDULE_TO_CLOSE, task.getProcessId(), task.getActorId(), task.getStartTime()));
        Connection connection = null;
        PreparedStatement ps = null;
        try {
            connection = dataSource.getConnection();
            ps = connection.prepareStatement("INSERT INTO PROCESS (process_id, start_task_id, custom_id, start_time, state) VALUES (?, ?, ?, ?, ?)");
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
        } finally {
            closeResources(ps, connection);
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
}
