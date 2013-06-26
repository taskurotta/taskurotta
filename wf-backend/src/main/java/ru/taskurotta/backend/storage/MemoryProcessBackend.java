package ru.taskurotta.backend.storage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.sf.cglib.core.CollectionUtils;
import net.sf.cglib.core.Predicate;
import ru.taskurotta.backend.checkpoint.CheckpointService;
import ru.taskurotta.backend.checkpoint.TimeoutType;
import ru.taskurotta.backend.checkpoint.impl.MemoryCheckpointService;
import ru.taskurotta.backend.checkpoint.model.Checkpoint;
import ru.taskurotta.backend.common.ObjectFactory;
import ru.taskurotta.backend.console.model.GenericPage;
import ru.taskurotta.backend.console.model.ProcessVO;
import ru.taskurotta.backend.console.retriever.ProcessInfoRetriever;
import ru.taskurotta.backend.dependency.model.DependencyDecision;
import ru.taskurotta.backend.snapshot.Snapshot;
import ru.taskurotta.backend.snapshot.SnapshotService;
import ru.taskurotta.backend.snapshot.SnapshotServiceImpl;
import ru.taskurotta.transport.model.DecisionContainer;
import ru.taskurotta.transport.model.TaskContainer;

/**
 * User: romario
 * Date: 4/2/13
 * Time: 8:02 PM
 */
public class MemoryProcessBackend implements ProcessBackend, ProcessInfoRetriever {

    private CheckpointService checkpointService = new MemoryCheckpointService();
    private Map<UUID, ProcessVO> processesStorage = new ConcurrentHashMap<>();
    private SnapshotService snapshotService;
    private ObjectFactory objectFactory;

    @Override
    public void startProcess(TaskContainer task) {
        ProcessVO process = new ProcessVO();
        process.setStartTime(System.currentTimeMillis());
        process.setProcessUuid(task.getProcessId());
        process.setStartTaskUuid(task.getTaskId());
        processesStorage.put(task.getProcessId(), process);

        Snapshot snapshot = new Snapshot();
        snapshot.setTask(objectFactory.parseTask(task));

        snapshotService.createSnapshot(snapshot);

        checkpointService.addCheckpoint(new Checkpoint(TimeoutType.PROCESS_SCHEDULE_TO_CLOSE, task.getProcessId(), task.getActorId(), task.getStartTime()));
        checkpointService.addCheckpoint(new Checkpoint(TimeoutType.PROCESS_START_TO_COMMIT, task.getProcessId(), task.getActorId(), task.getStartTime()));
    }

    @Override
    public void startProcessCommit(TaskContainer task) {

        //should be at the end of the method
        checkpointService.addCheckpoint(new Checkpoint(TimeoutType.PROCESS_START_TO_CLOSE, task.getProcessId(), task.getActorId(), task.getStartTime()));
        checkpointService.removeEntityCheckpoints(task.getProcessId(), TimeoutType.PROCESS_START_TO_COMMIT);
    }

    @Override
    public void finishProcess(DependencyDecision dependencyDecision, String returnValue) {

        ProcessVO process = processesStorage.get(dependencyDecision.getFinishedProcessId());
        process.setEndTime(System.currentTimeMillis());
        process.setReturnValueJson(returnValue);
        processesStorage.put(dependencyDecision.getFinishedProcessId(), process);

        Snapshot snapshot = new Snapshot();
        snapshot.setDependencyDecision(dependencyDecision);
        snapshotService.createSnapshot(snapshot);
        //should be at the end of the method
        checkpointService.removeEntityCheckpoints(dependencyDecision.getFinishedProcessId(), TimeoutType.PROCESS_START_TO_CLOSE);
        checkpointService.removeEntityCheckpoints(dependencyDecision.getFinishedProcessId(), TimeoutType.PROCESS_SCHEDULE_TO_CLOSE);
    }

    @Override
    public CheckpointService getCheckpointService() {
        return checkpointService;
    }

    public void setCheckpointService(CheckpointService checkpointService) {
        this.checkpointService = checkpointService;
    }

    public void setSnapshotService(SnapshotService snapshotService) {
        this.snapshotService = snapshotService;
    }

    @Override
    public ProcessVO getProcess(UUID processUUID) {
        return processesStorage.get(processUUID);
    }

    @Override
    public GenericPage<ProcessVO> listProcesses(int pageNumber, int pageSize) {
        List<ProcessVO> result = new ArrayList<>();
        ProcessVO[] processes = new ProcessVO[processesStorage.values().size()];
        processes = processesStorage.values().toArray(processes);
        if (!processesStorage.isEmpty()) {
            for (int i = (pageNumber - 1) * pageSize; i <= ((pageSize * pageNumber >= (processes.length)) ? (processes.length) - 1 : pageSize * pageNumber - 1); i++) {
                result.add(processes[i]);
            }
        }
        return new GenericPage<ProcessVO>(result, pageNumber, pageSize, processes.length);

    }

    @Override
    public List<ProcessVO> findProcesses(String type, final String id) {
        List<ProcessVO> result = new ArrayList<>();
        if ((id != null) && (!id.isEmpty())) {
            if (SEARCH_BY_ID.equals(type)) {
                result.addAll(CollectionUtils.filter(processesStorage.values(), new Predicate() {
                    @Override
                    public boolean evaluate(Object o) {
                        ProcessVO process = (ProcessVO) o;
                        return process.getProcessUuid().toString().startsWith(id);
                    }
                }));
            } else if (SEARCH_BY_CUSTOM_ID.equals(type)) {
                result.addAll(CollectionUtils.filter(processesStorage.values(), new Predicate() {
                    @Override
                    public boolean evaluate(Object o) {
                        ProcessVO process = (ProcessVO) o;
                        return process.getCustomId().startsWith(id);
                    }
                }));
            }
        }
        return result;
    }
}
