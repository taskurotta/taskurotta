package ru.taskurotta.backend.hz.dependency;

import ru.taskurotta.backend.dependency.GeneralDependencyBackend;
import ru.taskurotta.backend.dependency.links.GraphDao;

/**
 * User: stukushin
 * Date: 14.08.13
 * Time: 14:23
 */
public class HzDependencyBackend extends GeneralDependencyBackend {

    private static HzDependencyBackend instance;
    private static final Object instanceMonitor = 0;

    public HzDependencyBackend(GraphDao graphDao) {
        super(graphDao);
    }

    public static HzDependencyBackend createInstance(GraphDao graphDao) {
        synchronized (instanceMonitor) {
            if (instance == null) {
                instance = new HzDependencyBackend(graphDao);
                instanceMonitor.notifyAll();
            }
        }

        return instance;
    }

    public static HzDependencyBackend getInstance() throws InterruptedException {
        synchronized (instanceMonitor) {
            if (instance == null) {
                instanceMonitor.wait();
            }
        }

        return instance;
    }
}
