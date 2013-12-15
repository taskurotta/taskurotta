package ru.taskurotta.service.hz.dependency;

import ru.taskurotta.service.dependency.GeneralDependencyService;
import ru.taskurotta.service.dependency.links.GraphDao;

/**
 * User: stukushin
 * Date: 14.08.13
 * Time: 14:23
 */
public class HzDependencyService extends GeneralDependencyService {

    private static HzDependencyService instance;
    private static final Object instanceMonitor = new Object();

    public HzDependencyService(GraphDao graphDao) {
        super(graphDao);
    }

    public static HzDependencyService createInstance(GraphDao graphDao) {
        synchronized (instanceMonitor) {
            if (instance == null) {
                instance = new HzDependencyService(graphDao);
                instanceMonitor.notifyAll();
            }
        }

        return instance;
    }

    public static HzDependencyService getInstance() throws InterruptedException {
        synchronized (instanceMonitor) {
            if (instance == null) {
                instanceMonitor.wait();
            }
        }

        return instance;
    }
}
