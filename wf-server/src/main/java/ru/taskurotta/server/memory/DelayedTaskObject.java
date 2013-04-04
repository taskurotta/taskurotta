package ru.taskurotta.server.memory;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

import ru.taskurotta.server.model.TaskObject;

/**
 * User: romario
 * Date: 3/27/13
 * Time: 3:48 PM
 */
public class DelayedTaskObject implements Delayed {

    protected TaskObject taskObject;

    protected long startTime;

    public DelayedTaskObject(TaskObject taskObject) {

        this.taskObject = taskObject;
        this.startTime = taskObject.getStartTime();

        if (startTime != 0L) {
            System.out.println("DelayedTaskObject!!!" + taskObject);
        }
    }

    @Override
    public long getDelay(TimeUnit unit) {
        return unit.convert(startTime - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    public int compareTo(Delayed o) {
        return Long.valueOf(((DelayedTaskObject) o).startTime).compareTo(startTime);
    }
}
