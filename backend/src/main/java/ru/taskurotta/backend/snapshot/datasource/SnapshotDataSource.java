package ru.taskurotta.backend.snapshot.datasource;

import ru.taskurotta.backend.snapshot.Snapshot;
import ru.taskurotta.core.Task;
import ru.taskurotta.core.TaskDecision;

import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * User: greg
 */
public interface SnapshotDataSource {

    void save(Snapshot snapshot);

    Snapshot loadSnapshotById(UUID id);

    List<Snapshot> getSnapshotsForPeriod(Date startDate, Date endDate);

    public List<Snapshot> loadSnapshotsByProccessId(UUID id);

}
