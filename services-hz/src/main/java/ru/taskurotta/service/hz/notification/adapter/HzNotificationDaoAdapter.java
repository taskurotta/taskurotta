package ru.taskurotta.service.hz.notification.adapter;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IExecutorService;
import com.hazelcast.core.Member;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.service.console.model.GenericPage;
import ru.taskurotta.service.hz.notification.adapter.tasks.AddSubscriptionCallable;
import ru.taskurotta.service.hz.notification.adapter.tasks.AddTriggerCallable;
import ru.taskurotta.service.hz.notification.adapter.tasks.RemoveSubscriptionRunnable;
import ru.taskurotta.service.hz.notification.adapter.tasks.UpdateSubscriptionRunnable;
import ru.taskurotta.service.hz.notification.adapter.tasks.UpdateTriggerRunnable;
import ru.taskurotta.service.notification.dao.NotificationDao;
import ru.taskurotta.service.notification.model.NotificationTrigger;
import ru.taskurotta.service.notification.model.SearchCommand;
import ru.taskurotta.service.notification.model.Subscription;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.Future;

/**
 * Created on 15.06.2015.
 */
public class HzNotificationDaoAdapter implements NotificationDao {

    private static final Logger logger = LoggerFactory.getLogger(HzNotificationDaoAdapter.class);
    private IExecutorService executorService;
    private static NotificationDao notificationDao;

    private boolean isSharedStore = false;


    public HzNotificationDaoAdapter(NotificationDao notificationDao, HazelcastInstance hazelcastInstance, boolean isSharedStore) {
        this.notificationDao = notificationDao;
        this.isSharedStore = isSharedStore;
        if (!isSharedStore) {
            executorService = hazelcastInstance.getExecutorService(getClass().getName());
        }
        logger.debug("Using hazelcast cluster adapter for {} notification store", (isSharedStore? "shared": "separate"));
    }

    public static NotificationDao getRealNotificationsDao() {
        return notificationDao;
    }

    @Override
    public Subscription getSubscription(long id) {
        return notificationDao.getSubscription(id);
    }

    @Override
    public NotificationTrigger getTrigger(long id) {
        return notificationDao.getTrigger(id);
    }

    @Override
    public long addSubscription(Subscription subscription) {
        long result = -1l;
        if (isSharedStore) {
            result = notificationDao.addSubscription(subscription);
        } else {
            Map<Member, Future<Long>> nodesResults = executorService.submitToAllMembers(new AddSubscriptionCallable(subscription));

            for (Future<Long> nodeResultFuture: nodesResults.values()) {
                Long nodeResult = null;
                try {
                    nodeResult = nodeResultFuture.get();
                    if (nodeResult != null) {
                        long newResult = nodeResult.longValue();
                        if (result < 0 || result == newResult) {//new result or the same as on prev node: case OK
                            result = newResult;

                        } else {//different results from nodes: nodes unsync, error state
                            throw new IllegalStateException("Cannot execute addSubscription["+subscription+"], nodes are not synchronized!");
                        }
                    } else {
                        throw new IllegalStateException("Cannot execute addSubscription["+subscription+"], node result is null!");
                    }
                } catch (Exception e) {
                    logger.error("addSubscription[" + subscription + "] execution interrupted, possible nodes desynchronization", e);
                    result =  -1l;
                    break;
                }
            }
        }
        return result;
    }

    @Override
    public void removeSubscription(long id) {
        if (isSharedStore) {
            notificationDao.removeSubscription(id);
        } else {
            executorService.executeOnAllMembers(new RemoveSubscriptionRunnable(id));
        }
    }


    @Override
    public long addTrigger(NotificationTrigger trigger) {
        long result = -1l;
        if (isSharedStore) {
            result = notificationDao.addTrigger(trigger);
        } else {
            Map<Member, Future<Long>> nodesResults = executorService.submitToAllMembers(new AddTriggerCallable(trigger));

            for (Future<Long> nodeResultFuture: nodesResults.values()) {
                Long nodeResult = null;
                try {
                    nodeResult = nodeResultFuture.get();
                    if (nodeResult != null) {
                        long newResult = nodeResult.longValue();
                        if (result < 0 || result == newResult) {//new result or the same as on prev node: case OK
                            result = newResult;

                        } else {//different results from nodes: nodes unsync, error state
                            throw new IllegalStateException("Cannot execute addTrigger["+trigger+"], nodes are not synchronized!");
                        }
                    } else {
                        throw new IllegalStateException("Cannot execute addTrigger["+trigger+"], node result is null!");
                    }
                } catch (Exception e) {
                    logger.error("addTrigger[" + trigger + "] execution interrupted, possible nodes desynchronization", e);
                    result =  -1l;
                    break;
                }
            }

        }
        return result;
    }

    @Override
    public void updateSubscription(Subscription subscription, long id) {
        if (isSharedStore) {
            notificationDao.updateSubscription(subscription, id);
        } else {
            executorService.executeOnAllMembers(new UpdateSubscriptionRunnable(subscription, id));
        }
    }

    @Override
    public void updateTrigger(NotificationTrigger trigger, long id) {
        if (isSharedStore) {
            notificationDao.updateTrigger(trigger, id);
        } else {
            executorService.executeOnAllMembers(new UpdateTriggerRunnable(trigger, id));
        }
    }

    @Override
    public Collection<Subscription> listSubscriptions() {
        return notificationDao.listSubscriptions();
    }

    @Override
    public GenericPage<Subscription> listSubscriptions(SearchCommand command) {
        return notificationDao.listSubscriptions(command);
    }

    @Override
    public Collection<NotificationTrigger> listTriggers() {
        return notificationDao.listTriggers();
    }

    @Override
    public Collection<Subscription> listTriggerSubscriptions(NotificationTrigger trigger) {
        return notificationDao.listTriggerSubscriptions(trigger);
    }

    @Override
    public Collection<Long> listTriggerKeys() {
        return notificationDao.listTriggerKeys();
    }
}
