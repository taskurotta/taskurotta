package ru.taskurotta.bugtest.darg.worker;

import ru.taskurotta.annotation.Worker;

@Worker
public interface DArgWorker {

    public String getParam();
    public Integer getNumber(String param);

}
