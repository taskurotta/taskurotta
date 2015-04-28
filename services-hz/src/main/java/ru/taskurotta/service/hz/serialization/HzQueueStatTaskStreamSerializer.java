package ru.taskurotta.service.hz.serialization;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.StreamSerializer;
import ru.taskurotta.service.hz.console.HzQueueStatTask;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * User: stukushin
 * Date: 28.04.2015
 * Time: 16:28
 */
public class HzQueueStatTaskStreamSerializer implements StreamSerializer<HzQueueStatTask> {

    @Override
    public void write(ObjectDataOutput out, HzQueueStatTask object) throws IOException {
        ArrayList<String> queueNames = object.getQueueNames();
        String queueNamePrefix = object.getQueueNamePrefix();

        String[] strings;
        if (queueNames == null || queueNames.isEmpty()) {
            strings = null;
        } else {
            strings = (String[]) queueNames.toArray();
        }
        SerializationTools.writeStringArray(out, strings);
        SerializationTools.writeString(out, queueNamePrefix);
    }

    @Override
    public HzQueueStatTask read(ObjectDataInput in) throws IOException {
        ArrayList<String> queueNames;
        String[] strings = SerializationTools.readStringArray(in);
        if (strings != null) {
            queueNames = new ArrayList<>(Arrays.asList(strings));
        } else {
            queueNames = new ArrayList<>();
        }
        String queueNamePrefix = SerializationTools.readString(in);
        return new HzQueueStatTask(queueNames, queueNamePrefix);
    }

    @Override
    public int getTypeId() {
        return ObjectTypes.HZ_QUEUE_STAT_TASK;
    }

    @Override
    public void destroy() {

    }
}
