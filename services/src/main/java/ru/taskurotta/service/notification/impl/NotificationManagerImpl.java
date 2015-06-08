package ru.taskurotta.service.notification.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;
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
    private QueueInfoRetriever queueInfoRetriever;
    private InterruptedTasksService interruptedTasksService;
    private long pollTimeout;
    private long interruptedPeriod;

    public void execute() {
        StoredState state = loadState();
        List<String> newQueues = processVoidQueues(pollTimeout, state.getQueues());
        state.setQueues(newQueues);

        List<InterruptedTask> newTasks = processInterruptedTasks(interruptedPeriod, state.getTasks());
        state.setTasks(newTasks);

        state.setDate(new Date());
        saveState(state);
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

    StoredState loadState() {
        return null;
    }

    void saveState(StoredState state) {

    }

    List<String> processVoidQueues(long pollTimeout, List<String> oldValues) {
        List<String> result = new ArrayList<>();
        Map<Date, String> queues = queueInfoRetriever.getNotPollingQueues(pollTimeout);
        if (queues != null) {
            Collection<String> newValues = getFilteredQueueValues(queues.values(), oldValues);
            if (newValues != null) {
                List<Notification> notifications = createNotifications(newValues);
                if (notifications != null) {
                    for (Notification notification : notifications) {
                        emailSender.send(notification);
                    }
                }
            }
        }

        return result;
    }

    private List<Notification> createNotifications(Collection<String> newValues) {
        return null;
    }

    Collection<String> getFilteredQueueValues(Collection<String> target, Collection<String> stored) {
        Collection<String> result = target;
        if (target!=null && stored!=null) {
            result = new ArrayList<>();
            for (String val : target) {
                if (!stored.contains(val)) {
                    result.add(val);
                }
            }
        }
        return result;
    }

    Collection<InterruptedTask> getFilteredTaskValues(Collection<InterruptedTask> target, Collection<InterruptedTask> stored) {
        Collection<InterruptedTask> result = target;
        if (target!=null && stored != null) {
            result = new ArrayList<InterruptedTask>();
            for (InterruptedTask task : target) {
                if (!stored.contains(task)) {
                    result.add(task);
                }
            }
        }
        return result;
    }

    List<InterruptedTask> processInterruptedTasks(long period, List<InterruptedTask> oldValues) {
        return null;
    }

    Collection<InterruptedTask> getInterruptedTasks(SearchCommand searchCommand) {
        Collection<InterruptedTask> tasks = interruptedTasksService.find(searchCommand);
        return null;
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
}
