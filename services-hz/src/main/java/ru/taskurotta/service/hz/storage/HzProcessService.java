package ru.taskurotta.service.hz.storage;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import ru.taskurotta.service.console.model.GenericPage;
import ru.taskurotta.service.console.model.ProcessVO;
import ru.taskurotta.service.console.retriever.ProcessInfoRetriever;
import ru.taskurotta.service.console.retriever.command.ProcessSearchCommand;
import ru.taskurotta.service.storage.ProcessService;
import ru.taskurotta.transport.model.TaskContainer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Created with IntelliJ IDEA.
 * User: dimadin
 * Date: 13.06.13 16:00
 */
public class HzProcessService implements ProcessService, ProcessInfoRetriever {

    private String processesStorageMapName = "processesStorage";//default
    private HazelcastInstance hzInstance;


    public HzProcessService(HazelcastInstance hzInstance) {
        this.hzInstance = hzInstance;
    }

    @Override
    public void startProcess(TaskContainer task) {
        ProcessVO process = new ProcessVO();
        process.setStartTime(System.currentTimeMillis());
        process.setProcessUuid(task.getProcessId());
        process.setStartTaskUuid(task.getTaskId());
        process.setStartTask(task);
        hzInstance.getMap(processesStorageMapName).set(task.getProcessId(), process, 0, TimeUnit.NANOSECONDS);
    }

    @Override
    public void finishProcess(UUID processId, String returnValue) {

        // TODO: get map reference only one time
        IMap<UUID, ProcessVO> processMap = hzInstance.getMap(processesStorageMapName);
        ProcessVO process = processMap.get(processId);
        process.setEndTime(System.currentTimeMillis());
        process.setReturnValueJson(returnValue);
        processMap.set(processId, process, 0, TimeUnit.NANOSECONDS);
    }

    @Override
    public void deleteProcess(UUID processId) {
        hzInstance.getMap(processesStorageMapName).delete(processId);
    }

    @Override
    public ProcessVO getProcess(UUID processUUID) {
        IMap<UUID, ProcessVO> processesStorage = hzInstance.getMap(processesStorageMapName);
        return processesStorage.get(processUUID);
    }

    @Override
    public TaskContainer getStartTask(UUID processId) {
        IMap<UUID, ProcessVO> processesStorage = hzInstance.getMap(processesStorageMapName);
        return processesStorage.get(processId).getStartTask();
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
    public List<ProcessVO> findProcesses(final ProcessSearchCommand command) {
        IMap<UUID, ProcessVO> processesStorage = hzInstance.getMap(processesStorageMapName);
        List<ProcessVO> result = new ArrayList<>();

        if (!command.isEmpty()) {
            result.addAll(Collections2.filter(processesStorage.values(), new Predicate<ProcessVO>() {

                private boolean hasText(String target){
                    return target != null && target.trim().length()>0;
                }

                private boolean isValid (ProcessVO processVO) {
                    boolean isValid = true;
                    if (hasText(command.getCustomId())) {
                        isValid = processVO.getProcessUuid().toString().startsWith(command.getCustomId());
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

    public void setProcessesStorageMapName(String processesStorageMapName) {
        this.processesStorageMapName = processesStorageMapName;
    }
}