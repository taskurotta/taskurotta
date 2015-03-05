package ru.taskurotta.mongodb.driver;

import com.mongodb.DBEncoder;
import com.mongodb.DBEncoderFactory;
import ru.taskurotta.mongodb.driver.impl.BEncoder;

/**
 */
public class BEncoderFactory implements DBEncoderFactory {

    private BSerializationService bSerializationService;

    public BEncoderFactory(BSerializationService bSerializationService) {
        this.bSerializationService = bSerializationService;
    }

    @Override
    public DBEncoder create() {
        return new BEncoder(bSerializationService);
    }

}
