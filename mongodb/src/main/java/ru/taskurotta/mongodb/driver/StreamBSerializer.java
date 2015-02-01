package ru.taskurotta.mongodb.driver;

import com.hazelcast.nio.ObjectDataInput;

/**
 */
public interface StreamBSerializer<T> {

    public static final CString _ID = new CString("_id");

    void write(BDataOutput out, T object);

    T read(ObjectDataInput in);
}
