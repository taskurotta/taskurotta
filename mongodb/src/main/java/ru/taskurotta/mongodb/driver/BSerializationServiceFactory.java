package ru.taskurotta.mongodb.driver;

import ru.taskurotta.mongodb.driver.impl.BSerializationServiceImpl;

/**
 */
public class BSerializationServiceFactory {

    public static final BSerializationService newInstance() {
        return new BSerializationServiceImpl();
    }
}
