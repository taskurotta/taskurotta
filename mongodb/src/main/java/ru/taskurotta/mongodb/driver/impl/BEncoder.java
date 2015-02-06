package ru.taskurotta.mongodb.driver.impl;

import com.mongodb.DefaultDBEncoder;
import org.bson.BSONObject;
import ru.taskurotta.mongodb.driver.BDataOutput;
import ru.taskurotta.mongodb.driver.CString;
import ru.taskurotta.mongodb.driver.DBObjectСheat;
import ru.taskurotta.mongodb.driver.StreamBSerializer;

import java.util.Date;
import java.util.UUID;

import static org.bson.BSON.ARRAY;
import static org.bson.BSON.BINARY;
import static org.bson.BSON.B_UUID;
import static org.bson.BSON.DATE;
import static org.bson.BSON.EOO;
import static org.bson.BSON.NUMBER;
import static org.bson.BSON.NUMBER_INT;
import static org.bson.BSON.NUMBER_LONG;
import static org.bson.BSON.OBJECT;
import static org.bson.BSON.STRING;

/**
 */
public class BEncoder extends DefaultDBEncoder implements BDataOutput {

    public static final int ID_CACHE_SIZE = 1000;
    public static final CString[] ARRAY_INDEXES = new CString[ID_CACHE_SIZE];

    static {
        for (int i = 0; i < ARRAY_INDEXES.length; i++) {
            ARRAY_INDEXES[i] = new CString(Integer.toString(i));
        }
    }

    private StreamBSerializer streamBSerializer;

    protected BEncoder(StreamBSerializer streamBSerializer) {
        this.streamBSerializer = streamBSerializer;
    }

    protected boolean handleSpecialObjects(String name, BSONObject o) {
        if (o == null)
            throw new NullPointerException("can't save a null object");

        if (!(o instanceof DBObjectСheat)) {
            return false;
        }

        final int sizePos = _buf.getPosition();
        _buf.writeInt(0); // leaving space for this.  set it at the end

        streamBSerializer.write(this, ((DBObjectСheat) o).getObject());

        _buf.write(EOO);
        _buf.writeInt(sizePos, _buf.getPosition() - sizePos);

        return true;
    }


//    protected void addInt(CString name, int n) {
//        _buf.write(NUMBER_INT);
//        name.writeCString(_buf);
//        _buf.writeInt(n);
//    }
//
//
//    protected void addLong(CString name, long n) {
//        _buf.write(NUMBER_LONG);
//        name.writeCString(_buf);
//        _buf.writeLong(n);
//    }
//
//    protected void addDouble(CString name, double n) {
//        _buf.write(NUMBER);
//        name.writeCString(_buf);
//        _buf.writeDouble(n);
//    }
//
//    protected void addString(CString name, String n) {
//        _buf.write(STRING);
//        name.writeCString(_buf);
//        _buf.writeString(n);
//    }

    public static CString getIndexName(int i) {

        CString id;

        if (i > -1 && i < BEncoder.ID_CACHE_SIZE) {
            id = BEncoder.ARRAY_INDEXES[i];
        } else {
            id = new CString(i);
        }

        return id;
    }


    @Override
    public void writeInt(CString name, int value) {
        _buf.write(NUMBER_INT);
        name.writeCString(_buf);
        _buf.writeInt(value);
    }

    @Override
    public void writeInt(int i, int value) {
        writeInt(getIndexName(i), value);
    }

    @Override
    public void writeLong(CString name, long value) {
        _buf.write(NUMBER_LONG);
        name.writeCString(_buf);
        _buf.writeLong(value);
    }

    @Override
    public void writeLong(int i, long value) {
        writeLong(getIndexName(i), value);
    }

    @Override
    public void writeDouble(CString name, double value) {
        _buf.write(NUMBER);
        name.writeCString(_buf);
        _buf.writeDouble(value);
    }

    @Override
    public void writeDouble(int i, double value) {
        writeDouble(getIndexName(i), value);
    }


    @Override
    public void writeString(CString name, String value) {
        if (value == null) return;

        _buf.write(STRING);
        name.writeCString(_buf);
        _buf.writeString(value);
    }

    @Override
    public void writeString(int i, String value) {
        writeString(getIndexName(i), value);
    }

    @Override
    public void writeUUID(CString name, UUID value) {
        if (value == null) return;

        _buf.write(BINARY);
        name.writeCString(_buf);
        _buf.writeInt(16);
        _buf.write(B_UUID);
        _buf.writeLong(value.getMostSignificantBits());
        _buf.writeLong(value.getLeastSignificantBits());
    }

    @Override
    public void writeUUID(int i, UUID value) {
        writeUUID(getIndexName(i), value);
    }


    @Override
    public void writeDate(CString name, Date value) {
        if (value == null) return;
        _buf.write(DATE);
        name.writeCString(_buf);
        _buf.writeLong(value.getTime());
    }

    @Override
    public void writeDate(int i, Date value) {
        writeDate(getIndexName(i), value);
    }

    @Override
    public int writeObject(CString name) {
        _buf.write(OBJECT);
        name.writeCString(_buf);

        final int sizePos = _buf.getPosition();
        // will be filled by object size in writeObjectStop() method
        _buf.writeInt(0);

        return sizePos;
    }

    @Override
    public int writeObject(int i) {
        return writeObject(getIndexName(i));
    }

    @Override
    public void writeObjectStop(int sizePos) {
        _buf.write(EOO);
        _buf.writeInt(sizePos, _buf.getPosition() - sizePos);
    }

    @Override
    public int writeArray(CString name) {
        _buf.write(ARRAY);
        name.writeCString(_buf);

        final int sizePos = _buf.getPosition();
        // will be filled by object size in writeArrayStop() method
        _buf.writeInt(0);

        return sizePos;
    }

    @Override
    public int writeArray(int i) {
        return writeArray(getIndexName(i));
    }

    @Override
    public void writeArrayStop(int sizePos) {
        _buf.write(EOO);
        _buf.writeInt(sizePos, _buf.getPosition() - sizePos);
    }

}
