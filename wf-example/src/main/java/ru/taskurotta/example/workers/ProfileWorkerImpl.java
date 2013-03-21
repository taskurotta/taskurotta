package ru.taskurotta.example.workers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.example.Profile;

/**
 * User: stukushin
 * Date: 07.02.13
 * Time: 13:15
 */
public class ProfileWorkerImpl implements ProfileWorker {
    private static final Logger logger = LoggerFactory.getLogger(ProfileWorkerImpl.class);

    @Override
    public Profile getUserProfile(long userId) {
        logger.info(".getUserProfile(userId = [{}])", userId);

        return getProfileById(userId);
    }

    @Override
    public void blockNotification(long userId) {
        logger.info(".blockNotification(userId = [{}])", userId);

        Profile profile = getProfileById(userId);
        profile.setDeliveryType(Profile.DELIVERY_TYPE.BLOCKED);
    }

    /**
     * This is a fake method. It should return real Profile in production environment.
     */
    private Profile getProfileById(long userId) {

        Profile profile = new Profile();
        profile.setId(userId);
        profile.setEmail("test@domain.com");
        profile.setPhoneNumber("1234567890");
        profile.setDeliveryType(Profile.DELIVERY_TYPE.SMS);

        return profile;
    }
}
