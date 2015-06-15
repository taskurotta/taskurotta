package ru.taskurotta.service.hz.notification.adapter;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ILock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.service.notification.NotificationManager;

import java.io.Serializable;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * Created on 15.06.2015.
 */
public class HzNotificationManagerExecutor {

    private static NotificationManager notificationManager;
    private static ILock nodeLock;

    public static NotificationManager getNotificationsManager() {
        return notificationManager;
    }

    public static ILock getNotificationsLock() {
        return nodeLock;
    }

    public HzNotificationManagerExecutor(HazelcastInstance hzInstance, NotificationManager notificationManager, long periodMs) {
        this.notificationManager = notificationManager;
        this.nodeLock = hzInstance.getLock(getClass().getName() + ".lock");

        Executors.newSingleThreadScheduledExecutor((new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread result = new Thread(r);
                result.setDaemon(true);
                result.setName("HzNotificationManagerExecutor");
                return result;
            }
        })).scheduleAtFixedRate(new ExecuteNotificationsTask(), 0, periodMs, TimeUnit.MILLISECONDS);
    }


    public static class ExecuteNotificationsTask implements Runnable, Serializable {

        private static final Logger logger = LoggerFactory.getLogger(ExecuteNotificationsTask.class);

        @Override
        public void run() {
            ILock nodeLock = HzNotificationManagerExecutor.getNotificationsLock();
            try {
                if (nodeLock.tryLock()) {
                    HzNotificationManagerExecutor.getNotificationsManager().execute();
                }

            } catch (Throwable e) {
                logger.error("Error at notifier execution, skip iteration...", e);

            } finally {
                nodeLock.unlock();
            }

        }
    }

}
