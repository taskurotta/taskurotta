package ru.taskurotta.service.notification.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.util.StringUtils;
import ru.taskurotta.service.console.model.InterruptedTask;
import ru.taskurotta.service.console.model.SearchCommand;
import ru.taskurotta.service.console.retriever.QueueInfoRetriever;
import ru.taskurotta.service.notification.EmailSender;
import ru.taskurotta.service.notification.NotificationManager;
import ru.taskurotta.service.notification.model.Notification;
import ru.taskurotta.service.notification.model.NotificationConfig;
import ru.taskurotta.service.notification.model.StoredState;
import ru.taskurotta.service.storage.EntityStore;
import ru.taskurotta.service.storage.InterruptedTasksService;
import ru.taskurotta.util.NotificationUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Created on 08.06.2015.
 */
public class NotificationManagerImpl implements NotificationManager {

    private static final Logger logger = LoggerFactory.getLogger(NotificationManagerImpl.class);

    private EmailSender emailSender;
    private EntityStore<NotificationConfig> cfgStore;
    private EntityStore<StoredState> stateStore;
    private QueueInfoRetriever queueInfoRetriever;
    private InterruptedTasksService interruptedTasksService;
    private long pollTimeout;
    private long interruptedPeriod;

    public void execute() {
        StoredState oldState = loadState();

        List<String> newQueues = processVoidQueues(pollTimeout, oldState != null ? oldState.getQueues() : null);
        List<InterruptedTask> newTasks = processInterruptedTasks(interruptedPeriod, oldState!=null? oldState.getTasks() : null);

        StoredState newState = new StoredState();
        newState.setQueues(newQueues);
        newState.setTasks(newTasks);
        newState.setDate(new Date());
        if (oldState != null) {
            stateStore.update(newState, oldState.getId());
        } else {
            stateStore.add(newState);
        }
    }

    StoredState loadState() {
        StoredState result = null;
        Collection<Long> states = stateStore.getKeys();
        if (states!=null && !states.isEmpty()) {
            long id = states.iterator().next();
            result = stateStore.get(id);
            if (result!=null) {
                result.setId(id);
            }
        }
        return result;
    }

    List<String> processVoidQueues(long pollTimeout, List<String> oldValues) {
        List<String> result = new ArrayList<>();
        Map<Date, String> queues = queueInfoRetriever.getNotPollingQueues(pollTimeout);
        if (queues != null) {
            Collection<String> newValues = NotificationUtils.getFilteredQueueValues(queues.values(), oldValues);
            if (newValues != null) {
                result.addAll(newValues);
                List<Notification> notifications = createVoidQueuesNotifications(newValues);
                if (notifications != null) {
                    for (Notification notification : notifications) {
                        emailSender.send(notification);
                        logger.debug("Notification[{}] have been successfully send", notification);
                    }
                }
            }
        }

        return result.isEmpty()? null : result;
    }


    List<InterruptedTask> processInterruptedTasks(long period, List<InterruptedTask> oldValues) {
        List<InterruptedTask> result = new ArrayList<>();
        SearchCommand itdCommand = new SearchCommand();
        long current = System.currentTimeMillis();
        itdCommand.setEndPeriod(current);
        itdCommand.setStartPeriod(current-period);
        Collection<InterruptedTask> itdTasks = interruptedTasksService.find(itdCommand);
        if (itdTasks != null) {
            Collection<InterruptedTask> newValues = NotificationUtils.getFilteredTaskValues(itdTasks, oldValues);
            if (newValues != null) {
                result.addAll(newValues);
                List<Notification> notifications = createInterruptedTasksNotifications(newValues);
                if (notifications != null) {
                    for (Notification notification : notifications) {
                        emailSender.send(notification);
                        logger.debug("Notification[{}] have been successfully send", notification);
                    }
                }
            }
        }

        return result.isEmpty()? null : result;
    }

    private List<Notification> createVoidQueuesNotifications(Collection<String> newValues) {
        List<Notification> result = new ArrayList<>();
        Collection<Long> keys = cfgStore.getKeys();
        if (keys!=null) {
            for (long key : keys) {
                NotificationConfig cfg = cfgStore.get(key);
                if (cfg!=null && NotificationUtils.containsValueOfInterest(cfg.getActorIds(), newValues)) {
                    Notification notification = new Notification();
                    notification.setBody("Queue(s) have not been polled for too long. Please check if actor(s) still active. \n\r Queues are: " + newValues);
                    notification.setIsHtml(false);
                    notification.setIsMultipart(false);
                    notification.setSubject("Void queues alert");
                    notification.setSendFrom("Taskurotta notification service");
                    notification.setSendTo(StringUtils.collectionToCommaDelimitedString(cfg.getEmails()));
                    result.add(notification);
                }
            }

        }
        return result;
    }

    private List<Notification> createInterruptedTasksNotifications(Collection<InterruptedTask> newValues) {
        List<Notification> result = new ArrayList<>();
        Collection<Long> keys = cfgStore.getKeys();
        if (keys!=null) {
            for (long key : keys) {
                NotificationConfig cfg = cfgStore.get(key);
//                if (cfg!=null && NotificationUtils.containsValueOfInterest(cfg.getActorIds(), newValues)) {
//                    Notification notification = new Notification();
//                    notification.setBody("Queue(s) have not been polled for too long. Please check if actor(s) still active. \n\r Queues are: " + newValues);
//                    notification.setIsHtml(false);
//                    notification.setIsMultipart(false);
//                    notification.setSubject("Void queues alert");
//                    notification.setSendFrom("Taskurotta notification service");
//                    notification.setSendTo(StringUtils.collectionToCommaDelimitedString(cfg.getEmails()));
//                    result.add(notification);
//                }
            }

        }
        return result;
    }

    @Override
    public boolean start() {
        return false;
    }

    @Override
    public boolean stop() {
        return false;
    }

    @Override
    public int getStatus() {
        return 0;
    }

    @Override
    public NotificationConfig getConfig(long id) {
        return cfgStore.get(id);
    }

    @Override
    public long addConfig(NotificationConfig cfg) {
        return cfgStore.add(cfg);
    }

    @Override
    public void updateConfig(NotificationConfig cfg, long id) {
        cfgStore.update(cfg, id);
    }

    @Override
    public List<NotificationConfig> getConfigs() {
        return null;
    }

    @Required
    public void setCfgStore(EntityStore<NotificationConfig> cfgStore) {
        this.cfgStore = cfgStore;
    }

    @Required
    public void setQueueInfoRetriever(QueueInfoRetriever queueInfoRetriever) {
        this.queueInfoRetriever = queueInfoRetriever;
    }

    @Required
    public void setInterruptedTasksService(InterruptedTasksService interruptedTasksService) {
        this.interruptedTasksService = interruptedTasksService;
    }

    @Required
    public void setPollTimeout(long pollTimeout) {
        this.pollTimeout = pollTimeout;
    }

    @Required
    public void setInterruptedPeriod(long interruptedPeriod) {
        this.interruptedPeriod = interruptedPeriod;
    }

    @Required
    public void setEmailSender(EmailSender emailSender) {
        this.emailSender = emailSender;
    }

    @Required
    public void setStateStore(EntityStore<StoredState> stateStore) {
        this.stateStore = stateStore;
    }
}
