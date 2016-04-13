package ru.taskurotta.bootstrap;

import org.junit.Ignore;
import ru.taskurotta.client.TaskSpreader;
import ru.taskurotta.core.Task;
import ru.taskurotta.core.TaskDecision;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * User: stukushin
 * Date: 08.04.13
 * Time: 14:47
 */

@Ignore
public class TestTaskSpreader implements TaskSpreader {

    private Date attemptDate;
    private List<Task> tasks;

    public List<Integer> timeouts = new ArrayList<Integer>();

    public TestTaskSpreader() {}

    public TestTaskSpreader(List<Task> tasks) {
        this.tasks = tasks;
    }

    @Override
    public Task poll() {
        if (tasks == null || tasks.isEmpty()) {
            if (attemptDate == null) {
                attemptDate = new Date();
            }

            timeouts.add((int) (System.currentTimeMillis() - attemptDate.getTime()) / 1000);
            attemptDate = new Date();

            return null;
        } else {
            return tasks.remove(0);
        }
    }

    @Override
    public void release(TaskDecision taskDecision) {

    }

    @Override
    public void updateTimeout(UUID taskId, UUID processId, long timeout) {

    }
}
