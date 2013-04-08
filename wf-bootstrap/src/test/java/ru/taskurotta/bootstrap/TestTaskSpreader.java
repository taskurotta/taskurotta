package ru.taskurotta.bootstrap;

import org.junit.Ignore;
import ru.taskurotta.client.TaskSpreader;
import ru.taskurotta.core.Task;
import ru.taskurotta.core.TaskDecision;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * User: stukushin
 * Date: 08.04.13
 * Time: 14:47
 */

@Ignore
public class TestTaskSpreader implements TaskSpreader {

    private Date attemptDate;
    public List<Integer> timeouts = new ArrayList<Integer>();

    @Override
    public Task poll() {
        if (attemptDate == null) {
            attemptDate = new Date();
        }

        timeouts.add((int) (System.currentTimeMillis() - attemptDate.getTime()) / 1000);
        attemptDate = new Date();

        return null;
    }

    @Override
    public void release(TaskDecision taskDecision) {

    }
}
