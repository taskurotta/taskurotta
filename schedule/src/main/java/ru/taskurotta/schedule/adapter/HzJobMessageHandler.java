package ru.taskurotta.schedule.adapter;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ILock;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.schedule.JobConstants;
import ru.taskurotta.schedule.JobManager;
import ru.taskurotta.schedule.JobStore;
import ru.taskurotta.schedule.JobVO;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
    private JobStore jobStore;

    public HzJobMessageHandler(HazelcastInstance hzInstance, String scheduleTopicName, JobManager jobManager, JobStore jobStore) {
        this.jobManager = jobManager;
        this.jobStore = jobStore;

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

            synchronizeScheduledTasksWithStore();

        } catch (Throwable e) {
            logger.error("Error at schedule initialization!", e);
            if (regId!=null) {
                topic.removeMessageListener(regId);
            }
            nodeLock.unlock();
        }
    }


    public void synchronizeScheduledTasksWithStore() {
        Collection<Long> jobIds = jobStore.getJobIds();
        List<Long> resumedJobs = new ArrayList();
        if (jobIds!=null && !jobIds.isEmpty()) {
            for (Long jobId : jobIds) {
                JobVO sTask = jobStore.getJob(jobId);
                if (sTask!=null && STATUS_ACTIVE == sTask.getStatus()) {
                    jobManager.startJob(jobId);
                    resumedJobs.add(jobId);
                }
            }
        }
        logger.info("Resumed jobs on schedule after sync are [{}]", resumedJobs);
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
            if(ACTION_ACTIVATE.equals(action)) {
                jobManager.startJob(hzMessage.getId());

            } else if (ACTION_DEACTIVATE.equals(action)) {
                jobManager.stopJob(hzMessage.getId());

            } else if(ACTION_DELETE.equals(action)) {
                jobManager.stopJob(hzMessage.getId());
                jobStore.removeJob(hzMessage.getId());

            } else {
                logger.error("Unsupported action getted ["+action+"]");
            }
        }

    }


}
