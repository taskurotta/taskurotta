package ru.taskurotta.mongodb.driver;

import com.mongodb.DBDecoderFactory;

/**
 */
public interface BSerializationService {

    public void registerSerializer(StreamBSerializer serializer);

    public StreamBSerializer getSerializer(String objectClassName);

    public StreamBSerializer getSerializer(Class clazz);

    public DBDecoderFactory getDecoderFactory(String objectClassName);

    public DBDecoderFactory getDecoderFactory(Class objectClass);

    public DBDecoderFactory getDecoderFactory(StreamBSerializer streamBSerializer);
}
