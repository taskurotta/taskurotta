package ru.taskurotta.backend.ora.checkpoint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.backend.checkpoint.CheckpointService;
import ru.taskurotta.backend.checkpoint.TimeoutType;
import ru.taskurotta.backend.checkpoint.model.Checkpoint;
import ru.taskurotta.backend.checkpoint.model.CheckpointQuery;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import static ru.taskurotta.backend.ora.tools.SqlResourceCloser.*;


/**
 * User: greg
 */
public class OracleCheckpointService implements CheckpointService {

    private static final Logger logger = LoggerFactory.getLogger(OracleCheckpointService.class);

    private DataSource dataSource;

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void addCheckpoint(Checkpoint checkpoint) {
        Connection connection = null;
        PreparedStatement ps = null;
        try {
            connection = dataSource.getConnection();
            ps = connection.prepareStatement("insert into TR_CHECKPOINTS(CHECKPOINT_ID, ENTITY_TYPE, TYPE_TIMEOUT,CHECKPOINT_TIME) values (?,?,?,?)");
            ps.setString(1, checkpoint.getGuid().toString());
            ps.setString(2, checkpoint.getEntityType());
            ps.setString(3, checkpoint.getTimeoutType().toString());
            ps.setLong(4, checkpoint.getTime());
            ps.executeUpdate();
        } catch (SQLException ex) {
            logger.error("Database error", ex);
        } finally {
            closeResources(ps, connection);
        }
    }

    @Override
    public void addCheckpoints(TimeoutType timeoutType, List<Checkpoint> checkpoints) {
        for (Checkpoint checkpoint : checkpoints) {
            checkpoint.setTimeoutType(timeoutType);
            addCheckpoint(checkpoint);
        }

    }

    @Override
    public void removeCheckpoint(Checkpoint checkpoint) {
        Connection connection = null;
        PreparedStatement ps = null;
        try {
            connection = dataSource.getConnection();
            ps = connection.prepareStatement("delete from TR_CHECKPOINTS where CHECKPOINT_ID = ?");
            ps.setString(1, checkpoint.getGuid().toString());
            ps.executeUpdate();
        } catch (SQLException ex) {
            logger.error("Database error", ex);
        } finally {
            closeResources(ps, connection);
        }
    }

    @Override
    public void removeCheckpoints(TimeoutType timeoutType, List<Checkpoint> checkpoint) {
        for (Checkpoint check : checkpoint) {
            removeCheckpoint(check);
        }
    }

    @Override
    public List<Checkpoint> listCheckpoints(CheckpointQuery command) {
        return null;
    }

    @Override
    public int removeEntityCheckpoints(UUID uuid, TimeoutType timeoutType) {
        Connection connection = null;
        PreparedStatement ps = null;
        try {
            connection = dataSource.getConnection();
            ps = connection.prepareStatement("delete from TR_CHECKPOINTS where ENTITY_TYPE = ? and TYPE_TIMEOUT=?");
            ps.setString(1, uuid.toString());
            ps.setString(2, timeoutType.toString());
            return ps.executeUpdate();
        } catch (SQLException ex) {
            logger.error("Database error", ex);
        } finally {
            closeResources(ps, connection);
        }
        return 0;
    }
}
