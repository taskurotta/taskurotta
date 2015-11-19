package ru.taskurotta.service.hz.storage;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.query.PagingPredicate;
import com.hazelcast.query.Predicates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.service.common.ResultSetCursor;
import ru.taskurotta.service.console.model.GenericPage;
import ru.taskurotta.service.console.model.Process;
import ru.taskurotta.service.console.retriever.ProcessInfoRetriever;
import ru.taskurotta.service.console.retriever.command.ProcessSearchCommand;
import ru.taskurotta.service.hz.support.PredicateUtils;
import ru.taskurotta.service.storage.ProcessService;
import ru.taskurotta.transport.model.TaskContainer;
import ru.taskurotta.transport.utils.TransportUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Created with IntelliJ IDEA.
 * Date: 13.06.13 16:00
 */
public class HzProcessService extends AbstractHzProcessService implements ProcessService, ProcessInfoRetriever {

    private static final Logger logger = LoggerFactory.getLogger(HzProcessService.class);

    private static final String START_TIME_INDEX_NAME = "startTime";
    private static final String END_TIME_INDEX_NAME = "endTime";
    private static final String STATE_INDEX_NAME = "state";

    protected String processesStorageMapName;
    protected IMap<UUID, Process> processIMap;


    public HzProcessService(HazelcastInstance hzInstance, String processesStorageMapName) {
        super(hzInstance);

        this.processesStorageMapName = processesStorageMapName;
        this.processIMap = hzInstance.getMap(processesStorageMapName);

        // prevent index creation in MongoProcessService
        if (this.getClass().equals(HzProcessService.class)) {
            this.processIMap.addIndex(START_TIME_INDEX_NAME, true);
            this.processIMap.addIndex(END_TIME_INDEX_NAME, true);
            this.processIMap.addIndex(STATE_INDEX_NAME, false);
        }
    }

    @Override
    public ResultSetCursor findIncompleteProcesses(long recoveryTime, int limit) {
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
    public ResultSetCursor<UUID> findLostProcesses(long lastFinishedProcessDeleteTime, long lastAbortedProcessDeleteTime, int batchSize) {
        PagingPredicate pagingPredicate = new PagingPredicate(
                Predicates.or(
                    Predicates.and(
                        Predicates.between(START_TIME_INDEX_NAME, 0l, lastAbortedProcessDeleteTime),
                        Predicates.equal(STATE_INDEX_NAME, Process.ABORTED)),
                    Predicates.and(
                        Predicates.between(END_TIME_INDEX_NAME, 0l, lastFinishedProcessDeleteTime),
                        Predicates.equal(STATE_INDEX_NAME, Process.FINISH))
                ), batchSize);

        Collection<UUID> result = new ArrayList<>(processIMap.keySet(pagingPredicate));
        if (logger.isDebugEnabled()) {
            logger.debug("Found [{}] lost processes for last gc time [{]]", result.size(), lastFinishedProcessDeleteTime);
        }

        return new ResultSetCursor<UUID>() {
            @Override
            public Collection<UUID> getNext() {
                return result;
            }

            @Override
            public void close() throws IOException {

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

        finishProcessInternal(process, returnValue);
    }

    protected void finishProcessInternal(Process process, String returnValue) {

        process.setEndTime(System.currentTimeMillis());
        process.setReturnValue(returnValue);
        process.setState(Process.FINISH);
        processIMap.set(process.getProcessId(), process, 0, TimeUnit.NANOSECONDS);
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
        setProcessState(processId, -1, Process.ABORTED);
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
            int state = process.getState();

            if (state == newState) {
                return;
            }

            if (oldState != -1 && state != oldState) {
                logger.warn("#[{}]: can't set process state to {}, because process is not in {} state. Its value is " +
                        "{}", processId, newState, oldState, state);
                return;
            }

            process.setState(newState);
            processIMap.set(processId, process, 0, TimeUnit.NANOSECONDS);
        } finally {
            processIMap.unlock(processId);
        }
    }

    @Override
    public GenericPage<Process> findProcesses(final ProcessSearchCommand command) {
        Collection<Process> items = null;

        com.hazelcast.query.Predicate<UUID, Process> predicate = constructPredicate(command);
        if (predicate != null) {
            items = processIMap.values(predicate);
        } else {
            items = processIMap.values();
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Found [{}] items for predicate [{}] from processes map size[{}]", items.size(), predicate, processIMap.size());
        }

        if (command.getActorId() != null) {//TODO: add actorId field to Process entity
            items = Collections2.filter(items, new Predicate<Process>() {
                @Override
                public boolean apply(Process p) {
                    return p.getStartTask()!=null && p.getStartTask().getActorId()!=null && p.getStartTask().getActorId().startsWith(command.getActorId());
                }
            });
        }

        if (items != null && !items.isEmpty()) {
            int pageEnd = Math.min(command.getPageSize() * command.getPageNum(), items.size());
            int pageStart = (command.getPageNum() - 1) * command.getPageSize();
            return new GenericPage<Process>(new ArrayList<Process>(items).subList(pageStart, pageEnd), command.getPageNum(), command.getPageSize(), items.size());
        } else {
            return new GenericPage<Process>(null, command.getPageNum(), command.getPageSize(), 0);
        }
    }

    static com.hazelcast.query.Predicate<UUID, Process> constructPredicate(ProcessSearchCommand command) {
        List<com.hazelcast.query.Predicate> pList = new ArrayList<>();
        if (command.getProcessId() != null) {
            pList.add(PredicateUtils.getEqual("processId", UUID.fromString(command.getProcessId())));
        }
        if (command.getActorId() != null) {
            pList.add(PredicateUtils.getEqual("actorId", UUID.fromString(command.getActorId())));
        }
        if (command.getCustomId() != null) {
            pList.add(PredicateUtils.getStartsWith("customId", command.getCustomId()));
        }

        if (command.getState()>=0) {
            pList.add(PredicateUtils.getEqual("state", command.getState()));
        }

        if (command.getStartedFrom() > 0) {
            pList.add(PredicateUtils.getMoreThen("startTime", command.getStartedFrom()));
        }

        if (command.getStartedTill() > 0) {
            pList.add(PredicateUtils.getLessThen("startTime", command.getStartedTill()));
        }

        return PredicateUtils.combineWithAndCondition(pList);
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

    @Override
    public int getActiveCount(String actorId, String taskList) {
        int result = 0;
        if (actorId != null) {
            Collection<Process> processes = processIMap.values();
            for (Process process : processes) {
                if (process.getState() == Process.START && actorId.equals(process.getStartTask().getActorId()) && (taskList==null || taskList.equals(TransportUtils.getTaskList(process.getStartTask())))) {
                    result++;
                }
            }
        }

        return result;
    }

}