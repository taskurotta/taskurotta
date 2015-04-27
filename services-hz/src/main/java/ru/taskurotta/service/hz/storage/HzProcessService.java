package ru.taskurotta.service.hz.storage;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.query.Predicates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.service.common.ResultSetCursor;
import ru.taskurotta.service.console.model.GenericPage;
import ru.taskurotta.service.console.model.Process;
import ru.taskurotta.service.console.retriever.ProcessInfoRetriever;
import ru.taskurotta.service.console.retriever.command.ProcessSearchCommand;
import ru.taskurotta.service.storage.ProcessService;
import ru.taskurotta.transport.model.TaskContainer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Created with IntelliJ IDEA.
 * User: dimadin
 * Date: 13.06.13 16:00
 */
public class HzProcessService implements ProcessService, ProcessInfoRetriever {

    private static final Logger logger = LoggerFactory.getLogger(HzProcessService.class);

    private static final String START_TIME_INDEX_NAME = "startTime";
    private static final String STATE_INDEX_NAME = "state";

    protected String processesStorageMapName;
    protected IMap<UUID, Process> processIMap;


    public HzProcessService(HazelcastInstance hzInstance, String processesStorageMapName) {
        this.processesStorageMapName = processesStorageMapName;
        this.processIMap = hzInstance.getMap(processesStorageMapName);

        // prevent index creation in MongoProcessService
        if (this.getClass().equals(HzProcessService.class)) {
            this.processIMap.addIndex(START_TIME_INDEX_NAME, true);
            this.processIMap.addIndex(STATE_INDEX_NAME, false);
        }
    }

    @Override
    public ResultSetCursor findProcesses(long recoveryTime, int limit) {
        com.hazelcast.query.Predicate predicate = new Predicates.AndPredicate(
                new Predicates.BetweenPredicate(START_TIME_INDEX_NAME, 0l, recoveryTime),
                new Predicates.EqualPredicate(STATE_INDEX_NAME, Process.START));

        //PagingPredicate should be available in HZ 3.2
        final Collection<UUID> result = new ArrayList<>();
        if (limit > 0) {

            int cnt = 0;
            for (UUID item : processIMap.keySet(predicate)) {
                result.add(item);
                if (++cnt >= limit) {
                    break;
                }
            }
        } else {
            result.addAll(processIMap.keySet(predicate));
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Found [{}] incomplete processes for beforeTime[{]]", (result != null ? result.size() : null), recoveryTime);
        }

        return new ResultSetCursor() {

            Collection<UUID> localResult = result;

            @Override
            public void close() throws IOException {

            }

            @Override
            public Collection<UUID> getNext() {
                Collection<UUID> returnResult = localResult;
                localResult = Collections.EMPTY_LIST;
                return returnResult;
            }
        };
    }


    @Override
    public void startProcess(TaskContainer task) {
        Process process = new Process(task);
        processIMap.set(process.getProcessId(), process, 0, TimeUnit.NANOSECONDS);
    }

    @Override
    public void finishProcess(UUID processId, String returnValue) {
        Process process = getProcess(processId);

        if (process == null) {
            logger.error("#[{}]: can't finish process, because process not found in storage", processId);
            return;
        }

        process.setEndTime(System.currentTimeMillis());
        process.setReturnValue(returnValue);
        process.setState(Process.FINISH);
        processIMap.set(processId, process, 0, TimeUnit.NANOSECONDS);
    }

    @Override
    public void deleteProcess(UUID processId) {
        processIMap.delete(processId);
    }

    @Override
    public Process getProcess(UUID processId) {
        return processIMap.get(processId);
    }

    @Override
    public TaskContainer getStartTask(UUID processId) {
        Process process = getProcess(processId);

        if (process == null) {
            logger.warn("#[{}]: can't get process start task, because process not found in storage", processId);
            return null;
        }

        return process.getStartTask();
    }

    @Override
    public void markProcessAsBroken(UUID processId) {
        setProcessState(processId, Process.START, Process.BROKEN);
    }

    @Override
    public void markProcessAsStarted(UUID processId) {
        setProcessState(processId, Process.BROKEN, Process.START);
    }

    @Override
    public void markProcessAsAborted(UUID processId) {
        setProcessState(processId, Process.START, Process.ABORTED);
    }

    private void setProcessState(UUID processId, int oldState, int newState) {

        processIMap.lock(processId);

        try {
            Process process = getProcess(processId);

            if (process == null) {
                logger.warn("#[{}]: can't set process state to {}, because process not found in storage", processId,
                        newState);
                return;
            }

            if (process.getState() == newState) {
                return;
            }

            if (process.getState() != oldState) {
                logger.warn("#[{}]: can't set process state to {}, because process is not in {} state. Its value is " +
                        "{}", processId, newState, oldState, process.getState());
                return;
            }

            process.setState(newState);
            processIMap.set(processId, process, 0, TimeUnit.NANOSECONDS);
        } finally {
            processIMap.unlock(processId);
        }
    }

    @Override
    public GenericPage<Process> listProcesses(int pageNumber, int pageSize, final int status, final String typeFilter) {
        List<Process> result = new ArrayList<>();
        Collection<Process> values = null;
        if (!processIMap.isEmpty()) {

            if (status >= 0 || typeFilter != null) {
                values = Collections2.filter(processIMap.values(), new Predicate<Process>() {
                    @Override
                    public boolean apply(Process input) {
                        boolean result = false;
                        if (input != null) {
                            result = status >= 0 ? (input.getState() == status) : true;
                            if (result && typeFilter != null) {
                                String actorType = input.getStartTask() != null ? input.getStartTask().getActorId() : null;
                                result = actorType != null && actorType.startsWith(typeFilter);
                            }
                        }
                        return result;
                    }
                });
            } else {
                values = processIMap.values();
            }

            if (values != null && !values.isEmpty()) {
                int pageEnd = Math.min(pageSize * pageNumber, values.size());
                int pageStart = (pageNumber - 1) * pageSize;
                result.addAll(new ArrayList<>(values).subList(pageStart, pageEnd));
            }
        }

        return new GenericPage<>(result, pageNumber, pageSize, values != null ? values.size() : 0);
    }

    @Override
    public List<Process> findProcesses(final ProcessSearchCommand command) {
        List<Process> result = new ArrayList<>();

        if (!command.isEmpty()) {
            result.addAll(Collections2.filter(processIMap.values(), new Predicate<Process>() {

                private boolean hasText(String target) {
                    return target != null && target.trim().length() > 0;
                }

                private boolean isValid(Process process) {
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

        Collection<Process> processes = processIMap.values();

        for (Process process : processes) {
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
        int result = 0;

        Collection<Process> processes = processIMap.values();
        for (Process process : processes) {
            if (process.getState() == Process.BROKEN) {
                result++;
            }
        }

        return result;
    }
}