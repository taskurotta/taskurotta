package ru.taskurotta.schedule.adapter;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ILock;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.dropwizard.resources.Action;
import ru.taskurotta.schedule.JobConstants;
import ru.taskurotta.schedule.manager.JobManager;

/**
 * Dispatches and process scheduling events from every node's console web UI
 * to the cluster node currently responsible for scheduling.
 *
 * Only one cluster node at a time should run scheduled jobs
 * User: dimadin
 * Date: 24.09.13 18:12
 */
public class HzJobMessageHandler implements MessageListener<HzMessage>, JobConstants {

    private static final Logger logger = LoggerFactory.getLogger(HzJobMessageHandler.class);
    private HazelcastInstance hzInstance;

    private ILock nodeLock;
    private ITopic topic;

    private String scheduleTopicName;

    private JobManager jobManager;

    public HzJobMessageHandler(HazelcastInstance hzInstance, String scheduleTopicName, JobManager jobManager) {
        this.jobManager = jobManager;

        this.hzInstance = hzInstance;

        this.scheduleTopicName = scheduleTopicName;
        this.nodeLock = hzInstance.getLock(scheduleTopicName + ".lock");
        this.topic = hzInstance.getTopic(scheduleTopicName);

        new Thread(new Initializer(this)).start();

    }


    private static class Initializer implements Runnable {

        private HzJobMessageHandler nodeDispatcher;

        Initializer(HzJobMessageHandler nodeDispatcher) {
            this.nodeDispatcher = nodeDispatcher;
        }

        @Override
        public void run() {
            nodeDispatcher.performInitialization();
        }
    }


    public void performInitialization() {
        String regId = null;
        try {
            nodeLock.lock();
            regId = topic.addMessageListener(this);
            logger.debug("Current node registered as message listener with id[{}]. Scheduled tasks sync started.", regId);
            jobManager.synchronizeScheduledTasksWithStore();

        } catch (Throwable e) {
            logger.error("Error at schedule initialization!", e);
            if (regId!=null) {
                topic.removeMessageListener(regId);
            }
            nodeLock.unlock();
        }
    }

    public void dispatch(HzMessage message) {
        topic.publish(message);
    }

    @Override
    public void onMessage(Message<HzMessage> message) {

        HzMessage hzMessage = message.getMessageObject();
        logger.debug("Processing console event [{}]", hzMessage);

        if (hzMessage != null) {
            String action = hzMessage.getAction();
            if(Action.ACTIVATE.getValue().equals(action)) {
                jobManager.startJob(hzMessage.getId());

            } else if (Action.DEACTIVATE.getValue().equals(action)) {
                jobManager.stopJob(hzMessage.getId());

            } else if(Action.DELETE.getValue().equals(action)) {
                jobManager.removeJob(hzMessage.getId());

            } else {
                logger.error("Unsupported action getted ["+action+"]");
            }
        }

    }


}
