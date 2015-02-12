package ru.taskurotta.mongodb.driver.impl;

import com.mongodb.DefaultDBEncoder;
import org.bson.BSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.mongodb.driver.BDataOutput;
import ru.taskurotta.mongodb.driver.BSerializationService;
import ru.taskurotta.mongodb.driver.CString;
import ru.taskurotta.mongodb.driver.DBObjectCheat;
import ru.taskurotta.mongodb.driver.StreamBSerializer;

import java.util.Date;
import java.util.UUID;

import static org.bson.BSON.ARRAY;
import static org.bson.BSON.BINARY;
import static org.bson.BSON.BOOLEAN;
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

    private static final Logger logger = LoggerFactory.getLogger(BEncoder.class);

    private final BSerializationService bSerializationService;

    public BEncoder(BSerializationService bSerializationService) {
        this.bSerializationService = bSerializationService;
    }

    protected boolean handleSpecialObjects(String name, BSONObject o) {
        if (o == null)
            throw new NullPointerException("can't save a null object");

        if (!(o instanceof DBObjectCheat)) {
            return false;
        }

        Object obj = ((DBObjectCheat) o).getObject();
        if (obj == null) {
            throw new IllegalArgumentException("DBObjectCheat does not constance (null) object to encode");
        }

        StreamBSerializer streamBSerializer = bSerializationService.getSerializer(obj.getClass());

        final int sizePos = _buf.getPosition();
        _buf.writeInt(0); // leaving space for this.  set it at the end

        streamBSerializer.write(this, obj);

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
    public void writeInt(CString name, int value) {
        _buf.write(NUMBER_INT);
        name.writeCString(_buf);
        _buf.writeInt(value);
    }

    @Override
    public void writeInt(CString name, int value, int defValue) {
        if (value == defValue) {
            return;
        }
        writeInt(name, value);
    }

    @Override
    public void writeInt(int i, int value) {
        writeInt(CString.valueOf(i), value);
    }

    @Override
    public void writeInt(int i, int value, int defValue) {
        if (value == defValue) {
            return;
        }
        writeInt(CString.valueOf(i), value);
    }

    @Override
    public void writeLong(CString name, long value) {
        _buf.write(NUMBER_LONG);
        name.writeCString(_buf);
        _buf.writeLong(value);
    }

    @Override
    public void writeLong(CString name, long value, long defValue) {
        if (value == defValue) {
            return;
        }
        writeLong(name, value);
    }

    @Override
    public void writeLong(int i, long value) {
        writeLong(CString.valueOf(i), value);
    }

    @Override
    public void writeLong(int i, long value, long defValue) {
        if (value == defValue) {
            return;
        }
        writeLong(CString.valueOf(i), value);
    }

    @Override
    public void writeDouble(CString name, double value) {
        _buf.write(NUMBER);
        name.writeCString(_buf);
        _buf.writeDouble(value);
    }

    @Override
    public void writeDouble(CString name, double value, double defValue) {
        if (value == defValue) {
            return;
        }
        writeDouble(name, value);
    }

    @Override
    public void writeDouble(int i, double value) {
        writeDouble(CString.valueOf(i), value);
    }

    @Override
    public void writeDouble(int i, double value, double defValue) {
        if (value == defValue) {
            return;
        }
        writeDouble(CString.valueOf(i), value);
    }

    @Override
    public void writeBoolean(CString name, boolean value) {
        _buf.write(BOOLEAN);
        name.writeCString(_buf);
        _buf.write(value ? (byte) 0x1 : (byte) 0x0);
    }

    @Override
    public void writeBoolean(CString name, boolean value, boolean defValue) {
        if (value == defValue) {
            return;
        }
        writeBoolean(name, value);
    }

    @Override
    public void writeBoolean(int i, boolean value) {
        writeBoolean(CString.valueOf(i), value);
    }

    @Override
    public void writeBoolean(int i, boolean value, boolean defValue) {
        if (value == defValue) {
            return;
        }
        writeBoolean(CString.valueOf(i), value);
    }


    @Override
    public void writeString(CString name, String value) {
        if (value == null) return;

        _buf.write(STRING);
        name.writeCString(_buf);
        _buf.writeString(value);
    }

    @Override
    public void writeString(CString name, String value, String defValue) {
        if (value == null || (defValue != null && defValue.equals(value))) {
            return;
        }
        writeString(name, value);
    }

    @Override
    public void writeString(int i, String value) {
        writeString(CString.valueOf(i), value);
    }

    @Override
    public void writeString(int i, String value, String defValue) {
        if (value == null || (defValue != null && defValue.equals(value))) {
            return;
        }
        writeString(CString.valueOf(i), value);
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
    public void writeUUID(CString name, UUID value, UUID defValue) {
        if (value == null || (defValue != null && defValue.equals(value))) {
            return;
        }
        writeUUID(name, value);
    }

    @Override
    public void writeUUID(int i, UUID value) {
        writeUUID(CString.valueOf(i), value);
    }

    @Override
    public void writeUUID(int i, UUID value, UUID defValue) {
        if (value == null || (defValue != null && defValue.equals(value))) {
            return;
        }
        writeUUID(CString.valueOf(i), value);
    }


    @Override
    public void writeDate(CString name, Date value) {
        if (value == null) return;
        _buf.write(DATE);
        name.writeCString(_buf);
        _buf.writeLong(value.getTime());
    }

    @Override
    public void writeDate(CString name, Date value, Date defValue) {
        if (value == null || (defValue != null && defValue.equals(value))) {
            return;
        }
        writeDate(name, value);
    }

    @Override
    public void writeDate(int i, Date value) {
        writeDate(CString.valueOf(i), value);
    }

    @Override
    public void writeDate(int i, Date value, Date defValue) {
        if (value == null || (defValue != null && defValue.equals(value))) {
            return;
        }
        writeDate(CString.valueOf(i), value);
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
        return writeObject(CString.valueOf(i));
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
        return writeArray(CString.valueOf(i));
    }

    @Override
    public void writeArrayStop(int sizePos) {
        _buf.write(EOO);
        _buf.writeInt(sizePos, _buf.getPosition() - sizePos);
    }

}
