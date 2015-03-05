package ru.taskurotta.mongodb.driver.io;

import ru.taskurotta.mongodb.driver.domain.InnerPojo;
import ru.taskurotta.mongodb.driver.BDataInput;
import ru.taskurotta.mongodb.driver.BDataOutput;
import ru.taskurotta.mongodb.driver.CString;
import ru.taskurotta.mongodb.driver.StreamBSerializer;

/**
 */
public class InnerPojoStreamBSerializer  implements StreamBSerializer<InnerPojo> {

    public static final CString NAME = new CString("name");
    public static final CString ID = new CString("id");

    @Override
    public Class<InnerPojo> getObjectClass() {
        return InnerPojo.class;
    }

    @Override
    public void write(BDataOutput out, InnerPojo innerObj) {

        out.writeString(NAME, innerObj.getName());
        out.writeUUID(ID, innerObj.getId());

    }

    @Override
    public InnerPojo read(BDataInput in) {

        InnerPojo innerObj = new InnerPojo();

        innerObj.setId(in.readUUID(ID));
        innerObj.setName(in.readString(NAME));

        return innerObj;
    }
}
