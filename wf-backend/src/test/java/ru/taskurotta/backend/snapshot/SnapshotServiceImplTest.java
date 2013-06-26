package ru.taskurotta.backend.snapshot;


import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;
import ru.taskurotta.backend.dependency.links.Graph;
import ru.taskurotta.backend.snapshot.datasource.SnapshotDataSource;
import ru.taskurotta.internal.core.TaskDecisionImpl;
import ru.taskurotta.internal.core.TaskImpl;
import ru.taskurotta.internal.core.TaskOptionsImpl;
import ru.taskurotta.internal.core.TaskTargetImpl;
import ru.taskurotta.transport.model.DecisionContainer;
import ru.taskurotta.transport.model.TaskType;

import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

/**
 * User: greg
 */
public class SnapshotServiceImplTest {

    @Mock
    SnapshotDataSource snapshotDataSource;

    Snapshot snapshot;
    private SnapshotService snapshotService;

    @BeforeTest
    public void init() {
        MockitoAnnotations.initMocks(this);
        snapshot = new Snapshot(
                new TaskImpl(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        new TaskTargetImpl(TaskType.DECIDER_START, "", "", ""),
                        0,
                        1,
                        new Object[]{},
                        new TaskOptionsImpl(null)),
                new Graph(UUID.randomUUID(), UUID.randomUUID()),
                new DecisionContainer());
        when(snapshotDataSource.loadSnapshotById(snapshot.getSnapshotId())).thenReturn(snapshot);
        snapshotService = new SnapshotServiceImpl(snapshotDataSource);

    }

    @Test
    public void testCreateSnapshot() throws Exception {
        snapshotService.createSnapshot(snapshot);
        verify(snapshotDataSource).save(snapshot);
    }

    @Test
    public void testGetSnapshot() throws Exception {
        UUID snapshotId = snapshot.getSnapshotId();
        assertEquals(snapshot, snapshotService.getSnapshot(snapshotId));
    }
}
