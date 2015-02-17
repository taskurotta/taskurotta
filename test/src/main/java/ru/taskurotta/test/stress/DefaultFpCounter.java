package ru.taskurotta.test.stress;

import ru.taskurotta.server.GeneralTaskServer;

/**
 * Created on 16.02.2015.
 */
public class DefaultFpCounter implements FinishedProcessesCounter {

    @Override
    public long getFinishedCount() {
        return GeneralTaskServer.finishedProcessesCounter.get();
    }

}
