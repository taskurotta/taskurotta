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
import ru.taskurotta.schedule.JobVO;
import ru.taskurotta.schedule.adapter.HzJobManagerAdapter.ActionMessage;
import ru.taskurotta.schedule.manager.JobManager;

import java.io.Serializable;
import java.util.Collection;
import java.util.Date;

/**
 * Proxies JobManager's method calls except for start/stop/remove Job methods.
 * These calls are dispatched to the node owning the scheduler via Hazelcast topic
 * *
 * Only one cluster node at a time should run scheduled jobs.
 * So only one instance of this class on the node owning the scheduler holds the lock and listens to the topic
 *
 * Date: 10.12.13 13:39
 */
public class HzJobManagerAdapter implements JobManager, MessageListener<ActionMessage>, JobConstants {

    private static final Logger logger = LoggerFactory.getLogger(HzJobManagerAdapter.class);
    private HazelcastInstance hzInstance;

    private ILock nodeLock;
    private ITopic topic;

    private String scheduleTopicName;

    private JobManager jobManager;

    public HzJobManagerAdapter(HazelcastInstance hzInstance, String scheduleTopicName, JobManager jobManager) {
        this.jobManager = jobManager;
        this.hzInstance = hzInstance;

        this.scheduleTopicName = scheduleTopicName;
        this.nodeLock = hzInstance.getLock(scheduleTopicName + ".lock");
        this.topic = hzInstance.getTopic(scheduleTopicName);

        new Thread(new Initializer(this)).start();
    }

    private static class Initializer implements Runnable {

        private HzJobManagerAdapter adapter;

        Initializer(HzJobManagerAdapter adapter) {
            this.adapter = adapter;
        }

        @Override
        public void run() {
            adapter.performInitialization();
        }

    }

    public static class ActionMessage implements Serializable {

        private long id;
        private String action;

        public ActionMessage(){}

        public ActionMessage(long id, String action) {
            this.id = id;
            this.action = action;
        }

        public String getAction() {
            return action;
        }

        public void setAction(String action) {
            this.action = action;
        }

        public long getId() {
            return id;
        }

        public void setId(long id) {
            this.id = id;
        }

        @Override
        public String toString() {
            return "ActionMessage{" +
                    "id=" + id +
                    ", action='" + action + '\'' +
                    "} ";
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
            if (regId != null) {
                topic.removeMessageListener(regId);
            }
            nodeLock.unlock();
        }
    }

    @Override
    public void onMessage(Message<ActionMessage> message) {

        ActionMessage hzMessage = message.getMessageObject();
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

    @Override
    public boolean startJob(long id) {
        topic.publish(new ActionMessage(id, Action.ACTIVATE.getValue()));
        return true;
    }

    @Override
    public void removeJob(long id) {
        topic.publish(new ActionMessage(id, Action.DELETE.getValue()));
    }

    @Override
    public boolean stopJob(long id) {
        topic.publish(new ActionMessage(id, Action.DEACTIVATE.getValue()));
        return true;
    }

    @Override
    public Collection<Long> getJobIds() {
        return jobManager.getJobIds();
    }

    @Override
    public JobVO getJob(long id) {
        return jobManager.getJob(id);
    }

    @Override
    public void updateJobStatus(long id, int status) {
        jobManager.updateJobStatus(id, status);
    }

    @Override
    public void updateJob(JobVO jobVO) {
        jobManager.updateJob(jobVO);
    }

    @Override
    public void updateErrorCount(long jobId, int count, String message) {
        jobManager.updateErrorCount(jobId, count, message);
    }

    @Override
    public void synchronizeScheduledTasksWithStore() {
        jobManager.synchronizeScheduledTasksWithStore();
    }

    @Override
    public int getJobStatus(long id) {
        return jobManager.getJobStatus(id);
    }

    @Override
    public Date getNextExecutionTime(long id) {
        return jobManager.getNextExecutionTime(id);
    }

    @Override
    public boolean isActive(JobVO job) {
        return jobManager.isActive(job);
    }

    @Override
    public Collection<Long> getScheduledJobIds() {
        return jobManager.getScheduledJobIds();
    }

    @Override
    public long addJob(JobVO job) {
        return jobManager.addJob(job);
    }

}
