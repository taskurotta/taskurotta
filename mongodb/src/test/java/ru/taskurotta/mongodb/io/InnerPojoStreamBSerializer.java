package ru.taskurotta.mongodb.io;

import com.hazelcast.nio.ObjectDataInput;
import ru.taskurotta.mongodb.domain.InnerPojo;
import ru.taskurotta.mongodb.driver.BDataOutput;
import ru.taskurotta.mongodb.driver.CString;
import ru.taskurotta.mongodb.driver.StreamBSerializer;

/**
 */
public class InnerPojoStreamBSerializer  implements StreamBSerializer<InnerPojo> {

    public static final CString NAME = new CString("name");
    public static final CString ID = new CString("id");

    @Override
    public void write(BDataOutput out, InnerPojo innerObj) {

        out.writeString(NAME, innerObj.getName());
        out.writeUUID(ID, innerObj.getId());

    }

    @Override
    public InnerPojo read(ObjectDataInput in) {
        return null;
    }
}
