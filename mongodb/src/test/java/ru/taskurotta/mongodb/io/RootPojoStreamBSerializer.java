package ru.taskurotta.mongodb.io;

import com.hazelcast.nio.ObjectDataInput;
import ru.taskurotta.mongodb.domain.RootPojo;
import ru.taskurotta.mongodb.driver.BDataOutput;
import ru.taskurotta.mongodb.driver.CString;
import ru.taskurotta.mongodb.driver.StreamBSerializer;

/**
 */
public class RootPojoStreamBSerializer implements StreamBSerializer<RootPojo> {

    public static final CString I = new CString("i");
    public static final CString STR = new CString("str");
    public static final CString DATE = new CString("date");
    public static final CString UUID = new CString("uuid");
    public static final CString HOUSE = new CString("house");

    InnerPojoStreamBSerializer innerSerializer = new InnerPojoStreamBSerializer();

    @Override
    public void write(BDataOutput out, RootPojo rootObj) {
        out.writeLong(_ID, rootObj.getI());
        out.writeString(STR, rootObj.getStr());
        out.writeDate(DATE, rootObj.getDate());
        out.writeUUID(UUID, rootObj.getUuid());

        int label = out.writeObject(HOUSE);
        innerSerializer.write(out, rootObj.getHouse());
        out.writeObjectStop(label);

    }

    @Override
    public RootPojo read(ObjectDataInput in) {
        return null;
    }
}
