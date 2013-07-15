package ru.taskurotta.bugtest.darg.worker;

/**
 * Created with IntelliJ IDEA.
 * User: dimadin
 * Date: 15.07.13 17:15
 */
public class DArgWorkerImpl implements DArgWorker {

    @Override
    public Integer getNumber(String param) {
        return Integer.valueOf(1);
    }

}
