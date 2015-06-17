package ru.taskurotta.service.notification.handler.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.service.console.retriever.QueueInfoRetriever;
import ru.taskurotta.service.notification.EmailSender;
import ru.taskurotta.service.notification.handler.TriggerHandler;
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

    private long defaultPollTimeout;

    public VoidQueuesHandler(EmailSender emailSender, QueueInfoRetriever queueInfoRetriever, long defaultPollTimeout) {
        this.emailSender = emailSender;
        this.queueInfoRetriever = queueInfoRetriever;
        this.defaultPollTimeout = defaultPollTimeout;
    }

    @Override
    public String handleTrigger(String stateJson, Collection<Subscription> subscriptions, String cfgJson) {
        try {
            String result = null;
            if (subscriptions!=null && !subscriptions.isEmpty()) {
                Configuration cfg = cfgJson!=null? mapper.readValue(cfgJson, Configuration.class) : getDefaultCfg();
                Map<Date, String> voidQueues = queueInfoRetriever.getNotPollingQueues(cfg.pollTimeout);
                logger.debug("Void queues are [{}]", voidQueues);
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
                emailNotification.setSubject("TASKUROTTA: Void queues alert");
                emailNotification.setSendTo(NotificationUtils.toCommaDelimited(s.getEmails()));
                result.add(emailNotification);
            }

        }
        return result;
    }

    public Configuration getDefaultCfg() {
        Configuration result = new Configuration();
        result.pollTimeout = defaultPollTimeout;
        return result;
    }

    public static class Configuration {
        long pollTimeout;
    }
}
