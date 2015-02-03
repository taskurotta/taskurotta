package ru.taskurotta.mongodb.driver.impl;

import com.mongodb.DBEncoder;
import com.mongodb.DBEncoderFactory;
import ru.taskurotta.mongodb.driver.StreamBSerializer;

/**
 */
public class BEncoderFactory implements DBEncoderFactory {

    private StreamBSerializer streamBSerializer;

    public BEncoderFactory(StreamBSerializer streamBSerializer) {
        this.streamBSerializer = streamBSerializer;
    }

    // todo: consider to use pool of BEncoder objects
    @Override
    public DBEncoder create() {
        return new BEncoder(streamBSerializer);
    }

}
