package ru.taskurotta.mongodb.driver;

import com.mongodb.DBEncoderFactory;

/**
 */
public interface BSerializationService extends DBEncoderFactory {

    public void registerSerializers(Class clazz, StreamBSerializer serializer);
}
