package ru.taskurotta.backend.storage;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import ru.taskurotta.backend.checkpoint.CheckpointService;
import ru.taskurotta.backend.checkpoint.impl.MemoryCheckpointService;
import ru.taskurotta.backend.console.model.GenericPage;
import ru.taskurotta.backend.console.model.ProcessVO;
import ru.taskurotta.backend.console.retriever.ProcessInfoRetriever;
import ru.taskurotta.backend.console.retriever.command.ProcessSearchCommand;
import ru.taskurotta.transport.model.TaskContainer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * User: romario
 * Date: 4/2/13
 * Time: 8:02 PM
 */
public class MemoryProcessBackend implements ProcessBackend, ProcessInfoRetriever {

    private CheckpointService checkpointService = new MemoryCheckpointService();
    private Map<UUID, ProcessVO> processesStorage = new ConcurrentHashMap<>();


    @Override
    public void startProcess(TaskContainer task) {
        ProcessVO process = new ProcessVO();
        process.setStartTime(System.currentTimeMillis());
        process.setProcessUuid(task.getProcessId());
        process.setStartTaskUuid(task.getTaskId());
        processesStorage.put(task.getProcessId(), process);

        //checkpointService.addCheckpoint(new Checkpoint(TimeoutType.PROCESS_SCHEDULE_TO_CLOSE, task.getTaskId(), task.getProcessId(), task.getActorId(), task.getStartTime()));
        //checkpointService.addCheckpoint(new Checkpoint(TimeoutType.PROCESS_START_TO_COMMIT, task.getTaskId(), task.getProcessId(), task.getActorId(), task.getStartTime()));
    }

    @Override
    public void startProcessCommit(TaskContainer task) {

        //should be at the end of the method
        //checkpointService.addCheckpoint(new Checkpoint(TimeoutType.PROCESS_START_TO_CLOSE, task.getTaskId(), task.getProcessId(), task.getActorId(), task.getStartTime()));
        //checkpointService.removeTaskCheckpoints(task.getTaskId(), task.getProcessId(), TimeoutType.PROCESS_START_TO_COMMIT);
    }

    @Override
    public void finishProcess(UUID processId, String returnValue) {

        ProcessVO process = processesStorage.get(processId);
        process.setEndTime(System.currentTimeMillis());
        process.setReturnValueJson(returnValue);
        processesStorage.put(processId, process);

        //should be at the end of the method
        //checkpointService.removeTaskCheckpoints(process.getStartTaskUuid(), processId, TimeoutType.PROCESS_START_TO_CLOSE);
        //checkpointService.removeTaskCheckpoints(process.getStartTaskUuid(), processId, TimeoutType.PROCESS_SCHEDULE_TO_CLOSE);
    }

    @Override
    public CheckpointService getCheckpointService() {
        return checkpointService;
    }

    @SuppressWarnings("UnusedDeclaration")
    public void setCheckpointService(CheckpointService checkpointService) {
        this.checkpointService = checkpointService;
    }

    @Override
    public ProcessVO getProcess(UUID processUUID) {
        return processesStorage.get(processUUID);
    }

    @Override
    public TaskContainer getStartTask(UUID processId) {
        return processesStorage.get(processId).getStartTask();
    }

    @Override
    public GenericPage<ProcessVO> listProcesses(int pageNumber, int pageSize) {
        List<ProcessVO> result = new ArrayList<>();
        if (!processesStorage.isEmpty()) {
            ProcessVO[] processes = new ProcessVO[processesStorage.values().size()];
            processes = processesStorage.values().toArray(processes);
            int pageStart = (pageNumber - 1) * pageSize;
            int pageEnd = (pageSize * pageNumber >= processes.length) ? processes.length : pageSize * pageNumber;
            result.addAll(Arrays.asList(processes).subList(pageStart, pageEnd));
        }
        return new GenericPage<>(result, pageNumber, pageSize, processesStorage.values().size());

    }

    @Override
    public List<ProcessVO> findProcesses(final ProcessSearchCommand command) {
        List<ProcessVO> result = new ArrayList<>();
        if (command.getCustomId()!=null || command.getProcessId()!=null ) {
            result.addAll(Collections2.filter(processesStorage.values(), new Predicate<ProcessVO>() {

                private boolean hasText(String target){
                    return target != null && target.trim().length()>0;
                }

                private boolean isValid (ProcessVO processVO) {
                    boolean isValid = true;
                    if (hasText(command.getCustomId())) {
                        isValid = isValid && processVO.getProcessUuid().toString().startsWith(command.getCustomId());
                    }
                    if (hasText(command.getProcessId())) {
                        isValid = isValid && processVO.getProcessUuid().toString().startsWith(command.getProcessId());
                    }
                    return isValid;
                }

                @Override
                public boolean apply(ProcessVO processVO) {
                    return isValid(processVO);
                }

            }));
        }
        return result;
    }
}
