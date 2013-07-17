package ru.taskurotta.recipes.darg.worker;

import ru.taskurotta.annotation.Worker;

@Worker
public interface DArgWorker {

    public String getParam();

    public String processParams(String p1, String p2, String p3, String p4);

    public Integer getNumber(String param);

}
