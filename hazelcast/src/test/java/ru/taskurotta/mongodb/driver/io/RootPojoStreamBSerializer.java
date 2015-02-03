package ru.taskurotta.mongodb.driver.io;

import ru.taskurotta.mongodb.driver.domain.RootPojo;
import ru.taskurotta.mongodb.driver.BDataInput;
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
    public static final CString LONG_ARRAY = new CString("longArray");

    InnerPojoStreamBSerializer innerSerializer = new InnerPojoStreamBSerializer();

    @Override
    public Class<RootPojo> getObjectClass() {
        return RootPojo.class;
    }

    @Override
    public void write(BDataOutput out, RootPojo rootObj) {

        out.writeInt(_ID, rootObj.getI());
        out.writeString(STR, rootObj.getStr());
        out.writeDate(DATE, rootObj.getDate());
        out.writeUUID(UUID, rootObj.getUuid());

        int label = out.writeObject(HOUSE);
        innerSerializer.write(out, rootObj.getHouse());
        out.writeObjectStop(label);

        long[] longs = rootObj.getLongArray();

        if (longs != null) {
            label = out.writeArray(LONG_ARRAY);
            for (int i = 0; i < longs.length; i++) {
                out.writeLong(i, longs[i]);
            }
            out.writeArrayStop(label);
        }

    }

    @Override
    public RootPojo read(BDataInput in) {

        RootPojo obj = new RootPojo(in.readInt(_ID));

        obj.setDate(in.readDate(DATE));

        int label = in.readObject(HOUSE);
        if (label != -1) {
            obj.setHouse(innerSerializer.read(in));
            in.readObjectStop(label);
        }

        obj.setUuid(in.readUUID(UUID));

        label = in.readArray(LONG_ARRAY);
        if (label != -1) {
            int arraySize = in.readArraySize();
            long[] longs = new long[arraySize];

            for (int i = 0; i < arraySize; i++) {
                longs[i] = in.readLong(i);
            }

            obj.setLongArray(longs);

            in.readArrayStop(label);
        }

        obj.setStr(in.readString(STR));

        return obj;
    }
}
