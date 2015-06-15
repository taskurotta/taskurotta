package ru.taskurotta.service.notification.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.util.StringUtils;
import ru.taskurotta.service.console.model.InterruptedTask;
import ru.taskurotta.service.console.model.SearchCommand;
import ru.taskurotta.service.notification.EmailSender;
import ru.taskurotta.service.notification.TriggerHandler;
import ru.taskurotta.service.notification.model.EmailNotification;
import ru.taskurotta.service.notification.model.NotificationTrigger;
import ru.taskurotta.service.notification.model.Subscription;
import ru.taskurotta.service.storage.InterruptedTasksService;
import ru.taskurotta.util.NotificationUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Created on 10.06.2015.
 */
public class InterruptedTasksHanler implements TriggerHandler {

    private static final Logger logger = LoggerFactory.getLogger(InterruptedTasksHanler.class);
    private EmailSender emailSender;
    private InterruptedTasksService interruptedTasksService;
    private ObjectMapper mapper = new ObjectMapper();
    {
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Override
    public String handleTrigger(String stateJson, Collection<Subscription> subscriptions, String cfgJson) {
        try {
            String result = null;
            if (cfgJson!=null && subscriptions!=null && !subscriptions.isEmpty()) {
                Configuration cfg = mapper.readValue(cfgJson, Configuration.class);
                SearchCommand itdCommand = new SearchCommand();
                long current = System.currentTimeMillis();
                itdCommand.setEndPeriod(current);
                itdCommand.setStartPeriod(current-cfg.interruptedPeriod);
                Collection<InterruptedTask> failedTasks = interruptedTasksService.find(itdCommand);

                List<InterruptedTask> prevTasks = stateJson!=null? (List<InterruptedTask>) mapper.readValue(stateJson, new TypeReference<List<InterruptedTask>>() {}) : new ArrayList<InterruptedTask>();
                List<InterruptedTask> newTasks = failedTasks!=null? new ArrayList<InterruptedTask>(failedTasks): new ArrayList<InterruptedTask>();
                result = mapper.writeValueAsString(newTasks);

                NotificationUtils.excludeOldTasksValues(newTasks, prevTasks);
                if (newTasks != null && !newTasks.isEmpty()) {
                    List<EmailNotification> emailNotifications = createInterruptedTasksNotifications(newTasks, subscriptions);
                    if (emailNotifications != null) {
                        for (EmailNotification emailNotification : emailNotifications) {
                            emailSender.send(emailNotification);
                            logger.debug("Notification[{}] have been successfully send", emailNotification);
                        }
                    }
                }
            }
            return result;

        } catch (Throwable e) {
            logger.error("Cannot handle: stateJson["+stateJson+"], cfgJson["+cfgJson+"], subscriptions["+subscriptions+"]", e);
            return stateJson;
        }
    }

    @Override
    public String getTriggerType() {
        return NotificationTrigger.TYPE_FAILED_TASKS;
    }

    List<EmailNotification> createInterruptedTasksNotifications(Collection<InterruptedTask> newValues, Collection<Subscription> subscriptions) {
        List<EmailNotification> result = new ArrayList<>();
        for (Subscription s : subscriptions) {
            Set<String> actorIds = NotificationUtils.asActorIdList(newValues);
            Collection trackedActors = NotificationUtils.getTrackedValues(s.getActorIds(), actorIds);
            if (trackedActors != null) {
                EmailNotification emailNotification = new EmailNotification();
                emailNotification.setBody("Process(es) failed with error. \n\r Actors are: " + trackedActors);
                emailNotification.setIsHtml(false);
                emailNotification.setIsMultipart(false);
                emailNotification.setSubject("Failed process alert");
                emailNotification.setSendFrom("Taskurotta notification service");
                emailNotification.setSendTo(StringUtils.collectionToCommaDelimitedString(s.getEmails()));
                result.add(emailNotification);
            }
        }
        return result;
    }

    public static class Configuration {
        long interruptedPeriod;
    }

    @Required
    public void setEmailSender(EmailSender emailSender) {
        this.emailSender = emailSender;
    }

    @Required
    public void setInterruptedTasksService(InterruptedTasksService interruptedTasksService) {
        this.interruptedTasksService = interruptedTasksService;
    }
}
