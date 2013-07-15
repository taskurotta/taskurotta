package ru.taskurotta.bugtest.darg.worker;

import ru.taskurotta.annotation.Worker;

@Worker
public interface DArgWorker {

    public Integer getNumber(String param);

}
