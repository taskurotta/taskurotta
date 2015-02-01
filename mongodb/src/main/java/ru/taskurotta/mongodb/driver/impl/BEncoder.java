package ru.taskurotta.mongodb.driver.impl;

import com.mongodb.DefaultDBEncoder;
import org.bson.BSONObject;
import ru.taskurotta.mongodb.driver.BDataOutput;
import ru.taskurotta.mongodb.driver.CString;

import java.util.Date;
import java.util.UUID;

import static org.bson.BSON.BINARY;
import static org.bson.BSON.B_UUID;
import static org.bson.BSON.DATE;
import static org.bson.BSON.EOO;
import static org.bson.BSON.NUMBER_LONG;
import static org.bson.BSON.OBJECT;
import static org.bson.BSON.STRING;

/**
 */
public class BEncoder extends DefaultDBEncoder implements BDataOutput {

    private BSerializationServiceImpl serializationService;

    protected BEncoder(BSerializationServiceImpl serializationService) {
        this.serializationService = serializationService;
    }

    protected boolean handleSpecialObjects(String name, BSONObject o) {
        if (o == null)
            throw new NullPointerException("can't save a null object");

        if (!(o instanceof DBObjectСheat)) {
            return false;
        }

        final int sizePos = _buf.getPosition();
        _buf.writeInt(0); // leaving space for this.  set it at the end

        serializationService.writeObject(this, ((DBObjectСheat) o).getObject());

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

    @Override
    public void writeString(CString name, String value) {
        _buf.write(STRING);
        name.writeCString(_buf);
        _buf.writeString(value);
    }

    @Override
    public void writeUUID(CString name, UUID value) {
        _buf.write(BINARY);
        name.writeCString(_buf);
        _buf.writeInt(16);
        _buf.write(B_UUID);
        _buf.writeLong(value.getMostSignificantBits());
        _buf.writeLong(value.getLeastSignificantBits());
    }

    @Override
    public void writeLong(CString name, long value) {
        _buf.write(NUMBER_LONG);
        name.writeCString(_buf);
        _buf.writeLong(value);
    }

    @Override
    public void writeDate(CString name, Date value) {
        _buf.write(DATE);
        name.writeCString(_buf);
        _buf.writeLong(value.getTime());
    }

    @Override
    public int writeObject(CString name) {
        _buf.write(OBJECT);
        name.writeCString(_buf);

        final int sizePos = _buf.getPosition();
        _buf.writeInt(0);

        return sizePos;
    }

    @Override
    public void writeObjectStop(int sizePos) {
        _buf.write(EOO);
        _buf.writeInt(sizePos, _buf.getPosition() - sizePos);
    }

}
