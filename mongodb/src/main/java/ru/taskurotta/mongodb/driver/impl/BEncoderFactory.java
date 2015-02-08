package ru.taskurotta.mongodb.driver.impl;

import com.mongodb.DBEncoder;
import com.mongodb.DBEncoderFactory;
import ru.taskurotta.mongodb.driver.BSerializationService;

/**
 */
public class BEncoderFactory implements DBEncoderFactory {

    private BSerializationService bSerializationService;

    public BEncoderFactory(BSerializationService bSerializationService) {
        this.bSerializationService = bSerializationService;
    }

    // todo: consider to use pool of BEncoder objects
    @Override
    public DBEncoder create() {
        return new BEncoder(bSerializationService);
    }

}
