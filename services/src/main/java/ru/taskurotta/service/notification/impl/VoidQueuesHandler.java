package ru.taskurotta.service.notification.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.util.StringUtils;
import ru.taskurotta.service.console.retriever.QueueInfoRetriever;
import ru.taskurotta.service.notification.EmailSender;
import ru.taskurotta.service.notification.TriggerHandler;
import ru.taskurotta.service.notification.model.EmailNotification;
import ru.taskurotta.service.notification.model.NotificationTrigger;
import ru.taskurotta.service.notification.model.Subscription;
import ru.taskurotta.util.NotificationUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created on 10.06.2015.
 */
public class VoidQueuesHandler implements TriggerHandler {

    private static final Logger logger = LoggerFactory.getLogger(VoidQueuesHandler.class);

    private EmailSender emailSender;
    private QueueInfoRetriever queueInfoRetriever;
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
                Map<Date, String> voidQueues = queueInfoRetriever.getNotPollingQueues(cfg.pollTimeout);

                List<String> prevQueueNames = stateJson!=null? (List<String>)mapper.readValue(stateJson, new TypeReference<List<String>>() {}): new ArrayList<String>();
                List<String> newQueueNames = voidQueues!=null? new ArrayList<String>(voidQueues.values()) : new ArrayList<String>();
                result = mapper.writeValueAsString(newQueueNames);

                NotificationUtils.excludeOldValues(newQueueNames, prevQueueNames);
                if (newQueueNames != null && !newQueueNames.isEmpty()) {
                    List<EmailNotification> emailNotifications = createVoidQueuesNotifications(newQueueNames, subscriptions);
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
        return NotificationTrigger.TYPE_VOID_QUEUES;
    }


    List<EmailNotification> createVoidQueuesNotifications(Collection<String> queueNames, Collection<Subscription> subscriptions) {
        List<EmailNotification> result = new ArrayList<>();
        for (Subscription s : subscriptions) {
            Set<String> trackedQueues = NotificationUtils.getTrackedValues(s.getActorIds(), queueNames);
            if (trackedQueues != null) {
                EmailNotification emailNotification = new EmailNotification();
                emailNotification.setBody("Queue(s) have not been polled for too long. Please check if actor(s) still active. \n\r Queues are: " + trackedQueues);
                emailNotification.setIsHtml(false);
                emailNotification.setIsMultipart(false);
                emailNotification.setSubject("Void queues alert");
                emailNotification.setSendFrom("Taskurotta notification service");
                emailNotification.setSendTo(StringUtils.collectionToCommaDelimitedString(s.getEmails()));
                result.add(emailNotification);
            }

        }
        return result;
    }

    public static class Configuration {
        long pollTimeout;
    }

    @Required
    public void setEmailSender(EmailSender emailSender) {
        this.emailSender = emailSender;
    }

    @Required
    public void setQueueInfoRetriever(QueueInfoRetriever queueInfoRetriever) {
        this.queueInfoRetriever = queueInfoRetriever;
    }
}
