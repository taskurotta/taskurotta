package ru.taskurotta.service.storage;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import ru.taskurotta.service.console.model.GenericPage;
import ru.taskurotta.service.console.model.Process;
import ru.taskurotta.service.console.retriever.ProcessInfoRetriever;
import ru.taskurotta.service.console.retriever.command.ProcessSearchCommand;
import ru.taskurotta.transport.model.TaskContainer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * User: romario
 * Date: 4/2/13
 * Time: 8:02 PM
 */
public class MemoryProcessService implements ProcessService, ProcessInfoRetriever {

    private Map<UUID, Process> processesStorage = new ConcurrentHashMap<>();

    @Override
    public void startProcess(TaskContainer task) {
        Process process = new Process(task);
        processesStorage.put(process.getProcessId(), process);
    }

    @Override
    public void finishProcess(UUID processId, String returnValue) {
        Process process = processesStorage.get(processId);
        process.setEndTime(System.currentTimeMillis());
        process.setState(Process.FINISH);
        process.setReturnValue(returnValue);
        processesStorage.put(processId, process);
    }

    @Override
    public void deleteProcess(UUID processId) {
        processesStorage.remove(processId);
    }

    @Override
    public Process getProcess(UUID processUUID) {
        return processesStorage.get(processUUID);
    }

    @Override
    public TaskContainer getStartTask(UUID processId) {
        return processesStorage.get(processId).getStartTask();
    }

    @Override
    public void markProcessAsBroken(UUID processId) {
        setProcessState(processId, Process.BROKEN);
    }

    @Override
    public void markProcessAsStarted(UUID processId) {
        setProcessState(processId, Process.START);
    }

    public void setProcessState(UUID processId, int state) {
        Process process = processesStorage.get(processId);
        process.setState(state);
        processesStorage.put(processId, process);
    }

    @Override
    public GenericPage<Process> listProcesses(int pageNumber, int pageSize, final int status, final String typeFilter) {
        List<Process> result = new ArrayList<>();
        Collection<Process> values = null;
        if (!processesStorage.isEmpty()) {

            if (status >= 0 || typeFilter!=null) {
                values = Collections2.filter(processesStorage.values(), new Predicate<Process>() {
                    @Override
                    public boolean apply(Process input) {
                        boolean result = false;
                        if (input != null) {
                            result = status>=0? (input.getState() == status): true;
                            if (result && typeFilter != null) {
                                String actorType = input.getStartTask()!=null? input.getStartTask().getActorId(): null;
                                result = actorType!=null && actorType.startsWith(typeFilter);
                            }
                        }
                        return result;
                    }
                });
            } else {
                values = processesStorage.values();
            }

            if (values!=null && !values.isEmpty()) {
                int pageStart = (pageNumber - 1) * pageSize;
                int pageEnd = Math.min(pageSize * pageNumber, values.size());
                result.addAll(new ArrayList<>(values).subList(pageStart, pageEnd));
            }
        }

        return new GenericPage<>(result, pageNumber, pageSize, values!=null? values.size(): 0);
    }

    @Override
    public List<Process> findProcesses(final ProcessSearchCommand command) {
        List<Process> result = new ArrayList<>();
        if (command.getCustomId()!=null || command.getProcessId()!=null ) {
            result.addAll(Collections2.filter(processesStorage.values(), new Predicate<Process>() {

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

    @Override
    public int getFinishedCount(String customId) {
        int result = 0;
        Iterator<Process> iterator = processesStorage.values().iterator();
        while (iterator.hasNext()) {
            Process process = iterator.next();
            if (process.getState() == Process.FINISH) {
                if (customId == null || customId.equals(process.getCustomId())) {
                    result++;
                }

            }
        }
        return result;
    }
}
