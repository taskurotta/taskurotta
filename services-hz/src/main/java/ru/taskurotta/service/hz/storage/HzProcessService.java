package ru.taskurotta.service.hz.storage;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import ru.taskurotta.service.console.model.GenericPage;
import ru.taskurotta.service.console.model.Process;
import ru.taskurotta.service.console.retriever.ProcessInfoRetriever;
import ru.taskurotta.service.console.retriever.command.ProcessSearchCommand;
import ru.taskurotta.service.storage.ProcessService;
import ru.taskurotta.transport.model.TaskContainer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Created with IntelliJ IDEA.
 * User: dimadin
 * Date: 13.06.13 16:00
 */
public class HzProcessService implements ProcessService, ProcessInfoRetriever {

    private IMap<UUID, Process> processIMap;

    public HzProcessService(HazelcastInstance hzInstance, String processesStorageMapName) {
        this.processIMap = hzInstance.getMap(processesStorageMapName);
    }

    @Override
    public void startProcess(TaskContainer task) {
        Process process = new Process(task);
        processIMap.set(process.getProcessId(), process, 0, TimeUnit.NANOSECONDS);
    }

    @Override
    public void finishProcess(UUID processId, String returnValue) {
        Process process = processIMap.get(processId);
        process.setEndTime(System.currentTimeMillis());
        process.setReturnValue(returnValue);
        processIMap.set(processId, process, 0, TimeUnit.NANOSECONDS);
    }

    @Override
    public void deleteProcess(UUID processId) {
        processIMap.delete(processId);
    }

    @Override
    public Process getProcess(UUID processUUID) {
        return processIMap.get(processUUID);
    }

    @Override
    public TaskContainer getStartTask(UUID processId) {
        return processIMap.get(processId).getStartTask();
    }

    @Override
    public GenericPage<Process> listProcesses(int pageNumber, int pageSize, final int status) {
        List<Process> result = new ArrayList<>();
        Collection<Process> values = null;
        if (!processIMap.isEmpty()) {

            if (status>0) {
                values = Collections2.filter(processIMap.values(), new Predicate<Process>() {
                    @Override
                    public boolean apply(Process input) {
                        return input!=null && (input.getState() == status);
                    }
                });
            } else {
                values = processIMap.values();
            }

            if (values!=null && !values.isEmpty()) {
                int pageEnd = Math.min(pageSize * pageNumber, values.size());
                int pageStart = (pageNumber - 1) * pageSize;
                result.addAll(new ArrayList(values).subList(pageStart, pageEnd));
            }
        }

        return new GenericPage<>(result, pageNumber, pageSize, values!=null? values.size(): 0);
    }

    @Override
    public List<Process> findProcesses(final ProcessSearchCommand command) {
        List<Process> result = new ArrayList<>();

        if (!command.isEmpty()) {
            result.addAll(Collections2.filter(processIMap.values(), new Predicate<Process>() {

                private boolean hasText(String target){
                    return target != null && target.trim().length()>0;
                }

                private boolean isValid (Process process) {
                    boolean isValid = true;
                    if (hasText(command.getCustomId())) {
                        isValid = process.getProcessId().toString().startsWith(command.getCustomId());
                    }
                    if (hasText(command.getProcessId())) {
                        isValid = isValid && process.getProcessId().toString().startsWith(command.getProcessId());
                    }
                    return isValid;
                }

                @Override
                public boolean apply(Process process) {
                    return isValid(process);
                }

            }));
        }

        return result;
    }
}