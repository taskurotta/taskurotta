package ru.taskurotta.test.stress;

import ru.taskurotta.server.GeneralTaskServer;

/**
 * Created on 17.02.2015.
 */
public class DefaultBrokenProcesspCounter implements ProcessesCounter {

    @Override
    public int getCount() {
        return GeneralTaskServer.brokenProcessesCounter.get();
    }

}
