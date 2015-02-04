package ru.taskurotta.mongodb.driver.impl;

import com.mongodb.DBDecoder;
import com.mongodb.DBDecoderFactory;
import ru.taskurotta.mongodb.driver.StreamBSerializer;

/**
 */
public class BDecoderFactory implements DBDecoderFactory {

    private StreamBSerializer streamBSerializer;

    public BDecoderFactory(StreamBSerializer streamBSerializer) {
        this.streamBSerializer = streamBSerializer;
    }

    // todo: consider to use pool of BDecoder objects
    @Override
    public DBDecoder create() {
        return new BDecoder(streamBSerializer);
    }
}
