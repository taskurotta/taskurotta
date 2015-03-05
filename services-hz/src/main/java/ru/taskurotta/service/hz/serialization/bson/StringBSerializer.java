package ru.taskurotta.service.hz.serialization.bson;

import ru.taskurotta.mongodb.driver.BDataInput;
import ru.taskurotta.mongodb.driver.BDataOutput;
import ru.taskurotta.mongodb.driver.StreamBSerializer;

/**
 * Created by greg on 09/02/15.
 */
public class StringBSerializer implements StreamBSerializer<String> {

    @Override
    public Class<String> getObjectClass() {
        return String.class;
    }

    @Override
    public void write(BDataOutput out, String object) {
         out.writeString(_ID, object);
    }

    @Override
    public String read(BDataInput in) {
        return in.readString(_ID);
    }
}
