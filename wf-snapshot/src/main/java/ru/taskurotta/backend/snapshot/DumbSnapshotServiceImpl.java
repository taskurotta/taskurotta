package ru.taskurotta.backend.snapshot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.List;
import java.util.UUID;

/**
 * User: greg
 */
public class DumbSnapshotServiceImpl implements SnapshotService {
    private final static Logger logger = LoggerFactory.getLogger(DumbSnapshotServiceImpl.class);

    @Override
    public void createSnapshot(UUID processID) {
        logger.debug("Dumb snapshot created");
    }

    @Override
    public Snapshot getSnapshot(UUID snapshotId) {
        throw new NotImplementedException();
    }

    @Override
    public List<Snapshot> getSnapshotByProcessId(UUID snapshotId) {
        throw new NotImplementedException();
    }

    @Override
    public void saveSnapshot(Snapshot snapshot) {
        throw new NotImplementedException();
    }
}
