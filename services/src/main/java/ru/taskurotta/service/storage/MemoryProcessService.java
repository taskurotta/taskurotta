package ru.taskurotta.service.storage;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import ru.taskurotta.service.common.ResultSetCursor;
import ru.taskurotta.service.console.model.GenericPage;
import ru.taskurotta.service.console.model.Process;
import ru.taskurotta.service.console.retriever.ProcessInfoRetriever;
import ru.taskurotta.service.console.retriever.command.ProcessSearchCommand;
import ru.taskurotta.transport.model.TaskContainer;
import ru.taskurotta.transport.utils.TransportUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Date: 4/2/13 8:02 PM
 */
@Deprecated
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
    public ResultSetCursor<UUID> findIncompleteProcesses(long recoveryTime, int limit) {
        return new ResultSetCursor<UUID>() {
            @Override
            public Collection<UUID> getNext() {
                return Collections.EMPTY_LIST;
            }

            @Override
            public void close() throws IOException {
            }
        };
    }

    @Override
    public void lock(UUID processId) {

    }

    @Override
    public void unlock(UUID processId) {

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

    @Override
    public void markProcessAsAborted(UUID processId) {
        setProcessState(processId, Process.ABORTED);
    }

    public void setProcessState(UUID processId, int state) {
        Process process = processesStorage.get(processId);
        process.setState(state);
        processesStorage.put(processId, process);
    }

    @Override
    public GenericPage<Process> findProcesses(final ProcessSearchCommand command) {
        Collection<Process> items = new ArrayList<>();
        if (command.isFilterEmpty()) {
            items = processesStorage.values();
        } else {
            items = Collections2.filter(processesStorage.values(), new Predicate<Process>() {

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
                    if (hasText(command.getActorId())) {
                        isValid = isValid && process.getStartTask().getActorId().startsWith(command.getActorId());
                    }
                    if (command.getState() >= 0) {
                        isValid = isValid && process.getState() == command.getState();
                    }
                    if (command.getStartedFrom() > 0) {
                        isValid = isValid && process.getStartTime() >= command.getStartedFrom();
                    }
                    if (command.getStartedTill() > 0) {
                        isValid = isValid && process.getStartTime() <= command.getStartedTill();
                    }
                    return isValid;
                }

                @Override
                public boolean apply(Process process) {
                    return isValid(process);
                }

            });

        }

        if (items!=null && !items.isEmpty()) {
            int pageStart = (command.getPageNum() - 1) * command.getPageSize();
            int pageEnd = Math.min(command.getPageSize() * command.getPageNum(), items.size());
            return new GenericPage<>(new ArrayList<>(items).subList(pageStart, pageEnd), command.getPageNum(), command.getPageSize(), items.size());
        } else {
            return new GenericPage<>(null, command.getPageNum(), command.getPageSize(), 0);
        }

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

    @Override
    public int getBrokenProcessCount() {
        int count = 0;

        Collection<Process> processes = processesStorage.values();
        for (Process process : processes) {
            if (process.getState() == Process.BROKEN) {
                count++;
            }
        }

        return count;
    }

    @Override
    public int getActiveCount(String actorId, String taskList) {
        int count = 0;
        if (actorId != null) {
            Collection<Process> processes = processesStorage.values();
            for (Process process : processes) {
                if (process.getState() == Process.START && actorId.equals(process.getStartTask().getActorId()) && (taskList==null || taskList.equals(TransportUtils.getTaskList(process.getStartTask())))) {
                    count++;
                }
            }
        }

        return count;
    }

}
