package ru.taskurotta.test.stress;

import ru.taskurotta.server.GeneralTaskServer;

/**
 * Created on 16.02.2015.
 */
public class DefaultFinishedProcessCounter implements ProcessesCounter {

    @Override
    public int getCount() {
        return GeneralTaskServer.finishedProcessesCounter.get();
    }

}
