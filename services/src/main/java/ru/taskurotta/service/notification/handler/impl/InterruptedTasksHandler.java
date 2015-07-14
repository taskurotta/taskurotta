package ru.taskurotta.service.notification.handler.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.service.console.model.InterruptedTask;
import ru.taskurotta.service.console.model.SearchCommand;
import ru.taskurotta.service.notification.EmailSender;
import ru.taskurotta.service.notification.handler.TriggerHandler;
import ru.taskurotta.service.notification.model.EmailNotification;
import ru.taskurotta.service.notification.model.NotificationTrigger;
import ru.taskurotta.service.notification.model.Subscription;
import ru.taskurotta.service.storage.InterruptedTasksService;
import ru.taskurotta.util.NotificationUtils;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Created on 10.06.2015.
 */
public class InterruptedTasksHandler implements TriggerHandler {

    private static final Logger logger = LoggerFactory.getLogger(InterruptedTasksHandler.class);
    private EmailSender emailSender;
    private InterruptedTasksService interruptedTasksService;
    private ObjectMapper mapper = new ObjectMapper();
    private ScriptEngineManager scriptEngineManager;

    {
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    private long defaultInterruptedPeriod;

    public InterruptedTasksHandler(EmailSender emailSender, InterruptedTasksService interruptedTasksService, long defaultInterruptedPeriod) {
        this.emailSender = emailSender;
        this.interruptedTasksService = interruptedTasksService;
        this.defaultInterruptedPeriod = defaultInterruptedPeriod;

        this.scriptEngineManager = new ScriptEngineManager();
    }

    @Override
    public String handleTrigger(String stateJson, Collection<Subscription> subscriptions, String cfgJson) {
        try {
            String result = null;
            if (subscriptions != null && !subscriptions.isEmpty()) {
                Configuration cfg = cfgJson != null ? mapper.readValue(cfgJson, Configuration.class) : getDefaultConfiguration();
                SearchCommand itdCommand = new SearchCommand();
                long current = System.currentTimeMillis();
                itdCommand.setEndPeriod(current);
                itdCommand.setStartPeriod(current - cfg.interruptedPeriod);
                Collection<InterruptedTask> failedTasks = interruptedTasksService.find(itdCommand);

                List<InterruptedTask> prevTasks = stateJson != null ? (List<InterruptedTask>) mapper.readValue(stateJson, new TypeReference<List<InterruptedTask>>() {}) : new ArrayList<>();
                List<InterruptedTask> newTasks = failedTasks != null ? new ArrayList<>(failedTasks) : new ArrayList<>();
                result = mapper.writeValueAsString(newTasks);

                NotificationUtils.excludeOldTasksValues(newTasks, prevTasks);
                if (!newTasks.isEmpty()) {
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
            logger.error("Cannot handle: stateJson[" + stateJson + "], cfgJson[" + cfgJson + "], subscriptions[" + subscriptions + "]", e);
            return stateJson;
        }
    }

    @Override
    public String getTriggerType() {
        return NotificationTrigger.TYPE_FAILED_TASKS;
    }

    private List<EmailNotification> createInterruptedTasksNotifications(Collection<InterruptedTask> newValues, Collection<Subscription> subscriptions) {
        List<EmailNotification> result = new ArrayList<>();
        for (Subscription s : subscriptions) {
            Set<String> actorIds = NotificationUtils.asActorIdList(newValues);
//            Collection trackedActors = NotificationUtils.getTrackedValues(s.getActorIds(), actorIds);
            Collection<String> trackedActors = NotificationUtils.filterTrackedActors(newValues, s.getScript(), getScriptEngine());
            if (trackedActors != null) {
                EmailNotification emailNotification = new EmailNotification();
                emailNotification.setBody("Process(es) failed with error. \n\r Actors are: " + trackedActors);
                emailNotification.setIsHtml(false);
                emailNotification.setIsMultipart(false);
                emailNotification.setSubject("TASKUROTTA: Failed process alert");
                emailNotification.setSendTo(NotificationUtils.toCommaDelimited(s.getEmails()));
                result.add(emailNotification);
            }
        }
        return result;
    }

    private ScriptEngine getScriptEngine() {
        return scriptEngineManager.getEngineByName("nashorn");
    }

    private Configuration getDefaultConfiguration() {
        Configuration result = new Configuration();
        result.interruptedPeriod = defaultInterruptedPeriod;
        return result;
    }

    public static class Configuration {
        long interruptedPeriod;
    }

}
