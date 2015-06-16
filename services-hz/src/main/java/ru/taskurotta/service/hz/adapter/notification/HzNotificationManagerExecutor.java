package ru.taskurotta.service.hz.adapter.notification;

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

    private static final Logger logger = LoggerFactory.getLogger(HzNotificationManagerExecutor.class);

    private static NotificationManager notificationManager;
    private static ILock nodeLock;

    private boolean enabled = false;

    public static NotificationManager getNotificationsManager() {
        return notificationManager;
    }

    public static ILock getNotificationsLock() {
        return nodeLock;
    }

    public HzNotificationManagerExecutor(HazelcastInstance hzInstance, NotificationManager notificationManager, final long periodMs, boolean enabled) {
        if (enabled) {
            this.notificationManager = notificationManager;
            this.nodeLock = hzInstance.getLock(getClass().getName() + ".lock");

            Thread nodeLocker = new Thread(new Runnable() {
                @Override
                public void run() {
                    boolean locked = false;
                    while (!locked) {
                        locked = nodeLock.tryLock();
                        if (locked) {//this node is the notification executor now
                            Executors.newSingleThreadScheduledExecutor((new ThreadFactory() {
                                @Override
                                public Thread newThread(Runnable r) {
                                    Thread result = new Thread(r);
                                    result.setDaemon(false);
                                    result.setName("HzNotificationManagerExecutor");
                                    return result;
                                }
                            })).scheduleAtFixedRate(new ExecuteNotificationsTask(), 0, periodMs, TimeUnit.MILLISECONDS);
                            logger.info("Node lock for notification execution aquired");
                        } else {
                            try {
                                Thread.sleep(periodMs);
                            } catch (InterruptedException e) {
                                logger.warn("Node locker thread interrupted");
                            }
                        }
                    }
                }
            });
            nodeLocker.setDaemon(true);
            nodeLocker.setName("HzNotificationManagerExecutor: node locker");
            nodeLocker.start();
        }
    }

    public static class ExecuteNotificationsTask implements Runnable, Serializable {

        private static final Logger logger = LoggerFactory.getLogger(ExecuteNotificationsTask.class);

        @Override
        public void run() {
            ILock nodeLock = HzNotificationManagerExecutor.getNotificationsLock();
            try {
                if (nodeLock.isLocked()) {//this node is notification executor
                    HzNotificationManagerExecutor.getNotificationsManager().execute();
                } else {
                    logger.warn("Tried to execute notifications on unlocked node: unconsistent notifier state detected");
                    //Thread.currentThread().interrupt();
                }

            } catch (Throwable e) {
                logger.error("Error at notifier execution, skip iteration...", e);

            }
        }
    }

}
