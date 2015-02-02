package ru.taskurotta.mongodb.driver.impl;

import com.mongodb.DBDecoder;
import com.mongodb.DBDecoderFactory;
import ru.taskurotta.mongodb.driver.StreamBSerializer;

/**
 */
public class BDecoderFactory implements DBDecoderFactory {

    private Class rootObjectClass;
    private BDecoder decoder;

    public BDecoderFactory(BSerializationServiceImpl serializationService, StreamBSerializer streamBSerializer) {
        this.rootObjectClass = rootObjectClass;

        decoder = new BDecoder(serializationService, streamBSerializer);
    }

    @Override
    public DBDecoder create() {
        return decoder;
    }
}
