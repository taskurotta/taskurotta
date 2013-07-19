package ru.taskurotta.backend.hz.storage;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.backend.checkpoint.CheckpointService;
import ru.taskurotta.backend.checkpoint.TimeoutType;
import ru.taskurotta.backend.checkpoint.model.Checkpoint;
import ru.taskurotta.backend.console.model.GenericPage;
import ru.taskurotta.backend.console.model.ProcessVO;
import ru.taskurotta.backend.console.retriever.ProcessInfoRetriever;
import ru.taskurotta.backend.storage.ProcessBackend;
import ru.taskurotta.transport.model.TaskContainer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Created with IntelliJ IDEA.
 * User: dimadin
 * Date: 13.06.13 16:00
 */
public class HzProcessBackend implements ProcessBackend, ProcessInfoRetriever {
    private final static Logger logger = LoggerFactory.getLogger(HzProcessBackend.class);

    private CheckpointService checkpointService;
    private String processesStorageMapName = "processesStorage";//default
    private HazelcastInstance hzInstance;


    public HzProcessBackend(HazelcastInstance hzInstance) {
        this.hzInstance = hzInstance;
    }

    @Override
    public void startProcess(TaskContainer task) {
        ProcessVO process = new ProcessVO();
        process.setStartTime(System.currentTimeMillis());
        process.setProcessUuid(task.getProcessId());
        process.setStartTaskUuid(task.getTaskId());
        hzInstance.getMap(processesStorageMapName).put(task.getProcessId(), process);

        checkpointService.addCheckpoint(new Checkpoint(TimeoutType.PROCESS_SCHEDULE_TO_CLOSE, task.getTaskId(), task.getProcessId(), task.getActorId(), task.getStartTime()));
        checkpointService.addCheckpoint(new Checkpoint(TimeoutType.PROCESS_START_TO_COMMIT, task.getTaskId(), task.getProcessId(), task.getActorId(), task.getStartTime()));
    }

    @Override
    public void startProcessCommit(TaskContainer task) {

        //should be at the end of the method
        Checkpoint checkpoint = new Checkpoint(TimeoutType.PROCESS_START_TO_CLOSE, task.getTaskId(),task.getProcessId(), task.getActorId(), task.getStartTime());
        checkpointService.addCheckpoint(checkpoint);
        //logger.debug("PROCESS_CHECKPOINT: added checkpoint [{}]", checkpoint);
        checkpointService.removeTaskCheckpoints(task.getTaskId(), task.getProcessId(), TimeoutType.PROCESS_START_TO_COMMIT);
    }

    @Override
    public void finishProcess(UUID processId, String returnValue) {

        IMap<UUID, ProcessVO> processMap = hzInstance.getMap(processesStorageMapName);
        ProcessVO process = processMap.get(processId);
        process.setEndTime(System.currentTimeMillis());
        process.setReturnValueJson(returnValue);
        processMap.put(processId, process);

        //should be at the end of the method
        int removed = checkpointService.removeTaskCheckpoints(process.getStartTaskUuid(), processId, TimeoutType.PROCESS_START_TO_CLOSE);
        //logger.debug("PROCESS_CHECKPOINT: removed checkpoint for processId[{}], id[{}], type[{}], result [{}]", processId, process.getStartTaskUuid(), TimeoutType.PROCESS_START_TO_CLOSE, removed);
        checkpointService.removeTaskCheckpoints(process.getStartTaskUuid(), processId, TimeoutType.PROCESS_SCHEDULE_TO_CLOSE);
    }

    @Override
    public CheckpointService getCheckpointService() {
        return checkpointService;
    }

    public void setCheckpointService(CheckpointService checkpointService) {
        logger.debug("set checkpoint service [{}]", checkpointService);
        this.checkpointService = checkpointService;
    }

    @Override
    public ProcessVO getProcess(UUID processUUID) {
        IMap<UUID, ProcessVO> processesStorage = hzInstance.getMap(processesStorageMapName);
        return processesStorage.get(processUUID);
    }

    @Override
    public GenericPage<ProcessVO> listProcesses(int pageNumber, int pageSize) {
        IMap<UUID, ProcessVO> processesStorage = hzInstance.getMap(processesStorageMapName);
        List<ProcessVO> result = new ArrayList<>();
        ProcessVO[] processes = new ProcessVO[processesStorage.values().size()];
        processes = processesStorage.values().toArray(processes);
        if (!processesStorage.isEmpty()) {
            int pageEnd = pageSize * pageNumber >= processes.length ? processes.length : pageSize * pageNumber;
            int pageStart = (pageNumber - 1) * pageSize;
            result.addAll(Arrays.asList(processes).subList(pageStart, pageEnd));
        }
        return new GenericPage<>(result, pageNumber, pageSize, processes.length);

    }

    @Override
    public List<ProcessVO> findProcesses(String type, final String id) {
        IMap<UUID, ProcessVO> processesStorage = hzInstance.getMap(processesStorageMapName);
        List<ProcessVO> result = new ArrayList<>();

        if ((id != null) && (!id.isEmpty())) {

            switch (type) {
                case SEARCH_BY_ID:
                    result.addAll(Collections2.filter(processesStorage.values(), new Predicate<ProcessVO>() {
                        @Override
                        public boolean apply(ProcessVO processVO) {
                            return processVO.getProcessUuid().toString().startsWith(id);
                        }
                    }));
                    break;

                case SEARCH_BY_CUSTOM_ID:
                    result.addAll(Collections2.filter(processesStorage.values(), new Predicate<ProcessVO>() {
                        @Override
                        public boolean apply(ProcessVO processVO) {
                            return processVO.getCustomId().startsWith(id);
                        }
                    }));
                    break;

                default:
                    break;
            }
        }

        return result;
    }

    public void setProcessesStorageMapName(String processesStorageMapName) {
        this.processesStorageMapName = processesStorageMapName;
    }
}
