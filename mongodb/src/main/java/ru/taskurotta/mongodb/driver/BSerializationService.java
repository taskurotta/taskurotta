package ru.taskurotta.mongodb.driver;

import com.mongodb.DBDecoderFactory;
import com.mongodb.DBEncoderFactory;

/**
 */
public interface BSerializationService {

    public void registerSerializer(Class clazz, StreamBSerializer serializer);

    public DBEncoderFactory getEncoderFactory();

    public DBDecoderFactory getDecoderFactory(Class rootObjectClass);
}
