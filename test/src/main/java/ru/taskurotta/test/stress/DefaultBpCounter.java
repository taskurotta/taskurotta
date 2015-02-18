package ru.taskurotta.test.stress;

import ru.taskurotta.server.GeneralTaskServer;

/**
 * Created on 17.02.2015.
 */
public class DefaultBpCounter implements ProcessesCounter {

    @Override
    public long getCount() {
        return GeneralTaskServer.brokenProcessesCounter.get();
    }

}
