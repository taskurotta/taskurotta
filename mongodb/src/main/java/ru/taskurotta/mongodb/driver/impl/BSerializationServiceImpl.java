package ru.taskurotta.mongodb.driver.impl;

import com.mongodb.DBDecoderFactory;
import ru.taskurotta.mongodb.driver.BDataOutput;
import ru.taskurotta.mongodb.driver.BDecoderFactory;
import ru.taskurotta.mongodb.driver.BSerializationService;
import ru.taskurotta.mongodb.driver.StreamBSerializer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 */
public class BSerializationServiceImpl implements BSerializationService {

    private Map<Class, StreamBSerializer> serializersMap = new ConcurrentHashMap<>();

    private final BSerializationService childService;

    public BSerializationServiceImpl(StreamBSerializer[] streamBSerializers, BSerializationService childService) {
        this.childService = childService;

        for (StreamBSerializer serializer : streamBSerializers) {
            registerSerializer(serializer);
        }
    }

    public void registerSerializer(StreamBSerializer serializer) {
        serializersMap.put(serializer.getObjectClass(), serializer);
    }

    @Override
    public StreamBSerializer getSerializer(String objectClassName) {

        try {
            return getSerializer(Class.forName(objectClassName));
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Can not found class for name [" + objectClassName + "]", e);
        }
    }

    protected void writeObject(BDataOutput out, Object obj) {

        StreamBSerializer serializer = getSerializer(obj.getClass());
        if (serializer != null) {
            serializer.write(out, obj);
            return;
        }

        throw new IllegalArgumentException("Can not serialize " + obj.getClass().getName() + ". Serializer not " +
                "registered for this type of object");
    }

    public StreamBSerializer getSerializer(Class clazz) {

        StreamBSerializer serializer = serializersMap.get(clazz);

        if (serializer == null && childService != null) {
            serializer = childService.getSerializer(clazz);
        }

        if (serializer == null) {
            throw new IllegalArgumentException("Can not found serializer for class [" + clazz.getName() + "]");
        }
        return serializer;
    }


    @Override
    public DBDecoderFactory getDecoderFactory(String objectClassName) {

        try {
            final Class clazz = Class.forName(objectClassName);
            return getDecoderFactory(clazz);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Can not found class for name [" + objectClassName + "]", e);
        }
    }

    public DBDecoderFactory getDecoderFactory(Class objectClass) {
        return new BDecoderFactory(getSerializer(objectClass));
    }

    @Override
    public DBDecoderFactory getDecoderFactory(StreamBSerializer streamBSerializer) {
        return new BDecoderFactory(streamBSerializer);
    }

}
