package ru.taskurotta.bootstrap;

import org.junit.Ignore;
import ru.taskurotta.RuntimeProcessor;
import ru.taskurotta.core.Task;
import ru.taskurotta.core.TaskDecision;
import ru.taskurotta.internal.Heartbeat;
import ru.taskurotta.internal.core.TaskDecisionImpl;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * User: stukushin
 * Date: 10.04.13
 * Time: 14:57
 */

@Ignore
public class TestRuntimeProcessor implements RuntimeProcessor {

    private Date attemptDate;

    public List<Integer> timeouts = new ArrayList<Integer>();

    @Override
    public TaskDecision execute(Task task, Heartbeat heartbeat) {
        if (attemptDate == null) {
            attemptDate = new Date();
        }

        timeouts.add((int) (System.currentTimeMillis() - attemptDate.getTime()) / 1000);
        attemptDate = new Date();

        return new TaskDecisionImpl(task.getId(), task.getProcessId(), task.getPass(), new Throwable(), null);
    }

    @Override
    public Task[] execute(UUID taskId, UUID processId, Heartbeat heartbeat, Runnable runnable) {
        return new Task[0];
    }
}
