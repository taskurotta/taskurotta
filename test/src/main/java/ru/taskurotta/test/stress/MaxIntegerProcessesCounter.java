package ru.taskurotta.test.stress;

/**
 * User: stukushin
 * Date: 30.05.2015
 * Time: 16:28
 */

public class MaxIntegerProcessesCounter implements ProcessesCounter {
    @Override
    public long getCount() {
        return Integer.MAX_VALUE;
    }
}
