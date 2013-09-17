package ru.taskurotta.example.notification.decider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.annotation.Asynchronous;
import ru.taskurotta.core.Promise;
import ru.taskurotta.example.notification.Profile;
import ru.taskurotta.example.notification.workers.EmailWorkerClient;
import ru.taskurotta.example.notification.workers.ProfileWorkerClient;
import ru.taskurotta.example.notification.workers.SMSWorkerClient;

/**
 * User: stukushin
 * Date: 07.02.13
 * Time: 13:14
 */
public class NotificationDeciderImpl implements NotificationDecider {

    private static final Logger logger = LoggerFactory.getLogger(NotificationDeciderImpl.class);

    private ProfileWorkerClient userProfile;
    private EmailWorkerClient emailTransport;
    private SMSWorkerClient smsTransport;
    private NotificationDeciderImpl asynchronous;

    @Override
    public void sendMessage(long userId, String message) {
        logger.info(".sendMessage(userId = [{}], message = [{}])", userId, message);

        Promise<Profile> profilePromise = userProfile.getUserProfile(userId);
        Promise<Boolean> sendResultPromise = asynchronous.sendToTransport(profilePromise, message);
        asynchronous.blockOnFail(userId, sendResultPromise);
    }

    @Asynchronous
    public Promise<Boolean> sendToTransport(Promise<Profile> profilePromise, String message) {
        logger.info(".sendToTransport(profilePromise = [{}], message = [{}])", profilePromise, message);

        Profile profile = profilePromise.get();

        switch (profile.getDeliveryType()) {
            case SMS: {
                return smsTransport.send(profile.getPhoneNumber(), message);
            }
            case EMAIL: {
				logger.info("profile.getEmail() = {}", profile.getEmail());
                return emailTransport.send(profile.getEmail(), message);
            }

        }

        return Promise.asPromise(Boolean.TRUE);
    }


    @Asynchronous
    public void blockOnFail(long userId, Promise<Boolean> sendResultPromise) {
        logger.info(".blockOnFail(userId = [{}], sendResultPromise = [{}])", userId, sendResultPromise);

        if (!sendResultPromise.get()) {
            userProfile.blockNotification(userId);
        }
    }


    public void setAsynchronous(NotificationDeciderImpl asynchronous) {
        this.asynchronous = asynchronous;
    }

    public void setSmsTransport(SMSWorkerClient smsTransport) {
        this.smsTransport = smsTransport;
    }

    public void setEmailTransport(EmailWorkerClient emailTransport) {
        this.emailTransport = emailTransport;
    }

    public void setUserProfile(ProfileWorkerClient userProfile) {
        this.userProfile = userProfile;
    }
}
