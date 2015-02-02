package ru.taskurotta.mongodb.driver.impl;

import com.mongodb.DBDecoderFactory;
import com.mongodb.DBEncoder;
import com.mongodb.DBEncoderFactory;
import ru.taskurotta.mongodb.driver.BDataOutput;
import ru.taskurotta.mongodb.driver.BSerializationService;
import ru.taskurotta.mongodb.driver.StreamBSerializer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 */
public class BSerializationServiceImpl  implements BSerializationService {

    private Map<Class, StreamBSerializer> serializersMap = new ConcurrentHashMap<>();

    private BEncoder encoder;


    private DBEncoderFactory dbEncoderFactory;

    public BSerializationServiceImpl() {

        encoder = new BEncoder(this);

        dbEncoderFactory = new DBEncoderFactory() {

            @Override
            public DBEncoder create() {
                // todo create pool of DBEncoder objects
                return encoder;
            }
        };

    }

    public void registerSerializer(Class clazz, StreamBSerializer serializer) {
        serializersMap.put(clazz, serializer);
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

    protected StreamBSerializer getSerializer(Class clazz) {
        return serializersMap.get(clazz);
    }


    public DBEncoderFactory getEncoderFactory() {
        return dbEncoderFactory;
    }

    public DBDecoderFactory getDecoderFactory(Class rootObjectClass) {
        return new BDecoderFactory(this, serializersMap.get(rootObjectClass));
    }

}
