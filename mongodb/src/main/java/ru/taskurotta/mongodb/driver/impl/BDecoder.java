package ru.taskurotta.mongodb.driver.impl;

import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.DefaultDBDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.mongodb.driver.BDataInput;
import ru.taskurotta.mongodb.driver.CString;
import ru.taskurotta.mongodb.driver.DBObjectCheat;
import ru.taskurotta.mongodb.driver.StreamBSerializer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.UUID;

import static org.bson.BSON.ARRAY;
import static org.bson.BSON.BINARY;
import static org.bson.BSON.BOOLEAN;
import static org.bson.BSON.CODE;
import static org.bson.BSON.CODE_W_SCOPE;
import static org.bson.BSON.DATE;
import static org.bson.BSON.EOO;
import static org.bson.BSON.MAXKEY;
import static org.bson.BSON.MINKEY;
import static org.bson.BSON.NULL;
import static org.bson.BSON.NUMBER;
import static org.bson.BSON.NUMBER_INT;
import static org.bson.BSON.NUMBER_LONG;
import static org.bson.BSON.OBJECT;
import static org.bson.BSON.OID;
import static org.bson.BSON.REF;
import static org.bson.BSON.REGEX;
import static org.bson.BSON.STRING;
import static org.bson.BSON.SYMBOL;
import static org.bson.BSON.TIMESTAMP;
import static org.bson.BSON.UNDEFINED;

/**
 */
public class BDecoder extends DefaultDBDecoder implements BDataInput {

    private static final Logger logger = LoggerFactory.getLogger(BDecoder.class);

    private static final CString err = new CString("$err");
    private static final CString err1 = new CString("err");
    private static final CString err2 = new CString("errmsg");

    private static final boolean DEBUG = false;

    private StreamBSerializer streamBSerializer;

    private int currPosition;
    private Map<CString, CString> currNamesMap;

    private Stack<Integer> positionStack = new Stack<>();
    private Stack<Map<CString, CString>> namesMapStack = new Stack<>();

    private byte[] bytes;
    private Object rootObj;

    // todo: instead of rootObjectClass there should be
    protected BDecoder(StreamBSerializer streamBSerializer) {
        this.streamBSerializer = streamBSerializer;
    }

    public DBObject decode(byte[] b, DBCollection collection) {
        decodeInternal(b);
        try {
            return new DBObjectCheat(rootObj);
        } finally {
            clear();
            // todo: put decoder back to object poll
        }
    }

    private void clear() {
        currPosition = 0;
        currNamesMap = null;
        bytes = null;
        rootObj = null;
        positionStack.clear();
        namesMapStack.clear();
    }

    public DBObject decode(InputStream in, DBCollection collection) throws IOException {
        byte[] loadedBytes = readDocumentByteArray(in);

        if (loadedBytes == null) {
            return new DBObjectCheat(null);
        }

        boolean good = decodeInternal(loadedBytes);

        if (!good) {
            // todo: put 4 bytes of stream length back to stream head
            return super.decode(in, collection);
        }

        try {
            return new DBObjectCheat(rootObj);
        } finally {
            clear();
            // todo: put decoder back to object poll
        }
    }

//    public static byte[] readDocumentByteArray(InputStream in) throws IOException {
//        byte[] bytes4Int = new byte[4];
//        in.read(bytes4Int);
//
//        int size = readInt(bytes4Int, 0);
//        if (size - 4 < 1) {
//            return null;
//        }
//
//        byte[] allStreamInBytes = new byte[size - 4];
//        in.read(allStreamInBytes);
//
//        return allStreamInBytes;
//    }

    public static int readInt(byte[] data, int offset) {
        int x = 0;
        x |= (0xFF & data[offset + 0]) << 0;
        x |= (0xFF & data[offset + 1]) << 8;
        x |= (0xFF & data[offset + 2]) << 16;
        x |= (0xFF & data[offset + 3]) << 24;
        return x;
    }

    private boolean decodeInternal(byte[] loadedBytes) {

        if (loadedBytes == null) {
            throw new IllegalStateException("not ready");
        }

        this.bytes = loadedBytes;
        currPosition = 0;
        currNamesMap = getPairNames();

        if (currNamesMap == null) {
            return false;
        }

        rootObj = streamBSerializer.read(this);
        return true;
    }

    public static void fillByteArray(InputStream in, byte b[], int len) throws IOException {

        int off = 0;

        while (len > 0) {
            final int x = in.read(b, off, len);
            if (x == -1) {
                throw new IllegalStateException("Can not read " + len + " bytes. Read result is " + off);
            }
            off += x;
            len -= x;
        }
    }

    private static byte writeByteTo(OutputStream out, InputStream in, byte[] tmp) throws IOException {
        fillByteArray(in, tmp, 1);
        out.write(tmp, 0, 1);
        return tmp[0];
    }


    private static int writeIntTo(OutputStream out, InputStream in, byte[] tmp) throws IOException {
        fillByteArray(in, tmp, 4);
        out.write(tmp, 0, 4);
        return readInt(tmp, 0);
    }

    private static void writeLess128BytesTo(OutputStream out, int size, InputStream in, byte[] tmp) throws IOException {
        fillByteArray(in, tmp, size);
        out.write(tmp, 0, size);
    }

    private static void writeMore128BytesTo(OutputStream out, int size, InputStream in, byte[] tmp) throws IOException {

        int sum = size;

        do {

            if (sum <= tmp.length) {
                fillByteArray(in, tmp, sum);
                out.write(tmp, 0, sum);
                return;
            }

            sum -= 128;
            fillByteArray(in, tmp, 128);
            out.write(tmp, 0, 128);
        } while (true);

    }

    public static byte[] readDocumentByteArray(InputStream in) throws IOException {
        byte[] tmp = new byte[4];
        fillByteArray(in, tmp, 4);

        int size = readInt(tmp, 0);
        if (size - 4 < 1) {
            return null;
        }

        // try to read whole array at one time
        if (size < 1000) {
            byte[] allStreamInBytes = new byte[size - 4];
            fillByteArray(in, allStreamInBytes, size - 4);

            return allStreamInBytes;

            // some thing wrong: никому верить нелья!
            // try to read carefully. Some times size has a wrong big value and we are welcome to OOM
        } else {

            logger.warn("Too big size of array [" + size + "]. Try to read carefully");

            tmp = new byte[128];
            ByteArrayOutputStream out = new ByteArrayOutputStream(1000);

            while (true) {

                final byte type = writeByteTo(out, in, tmp);

                if (type == EOO) {
                    break;
                }

                // skip name
                while (writeByteTo(out, in, tmp) != 0) ;

                switch (type) {
                    case NULL:
                        break;

                    case UNDEFINED:
                        break;

                    case BOOLEAN:
                        writeLess128BytesTo(out, 1, in, tmp);
                        break;

                    case NUMBER:
                        writeLess128BytesTo(out, 8, in, tmp);
                        break;

                    case NUMBER_INT:
                        writeLess128BytesTo(out, 4, in, tmp);
                        break;

                    case NUMBER_LONG:
                        writeLess128BytesTo(out, 8, in, tmp);
                        break;

                    case SYMBOL:
                        writeMore128BytesTo(out, writeIntTo(out, in, tmp), in, tmp);
                        break;

                    case STRING:
                        writeMore128BytesTo(out, writeIntTo(out, in, tmp), in, tmp);
                        break;

                    case OID:
                        // ObjectId 3 * int
                        writeLess128BytesTo(out, 12, in, tmp);
                        break;

                    case REF:
                        // length of CString that follows
                        // ObjectId 3 * int
                        writeMore128BytesTo(out, writeIntTo(out, in, tmp) + 12, in, tmp);
                        break;

                    case DATE:
                        writeLess128BytesTo(out, 8, in, tmp);
                        break;

                    case REGEX:
                        // skip 2 CString
                        // _callback.gotRegex(name, _in.readCStr(), _in.readCStr());
                        while (writeByteTo(out, in, tmp) != 0) ;
                        while (writeByteTo(out, in, tmp) != 0) ;
                        break;

                    case BINARY:
                        writeMore128BytesTo(out, writeIntTo(out, in, tmp) + 1, in, tmp);
                        break;

                    case CODE:
                        writeMore128BytesTo(out, writeIntTo(out, in, tmp), in, tmp);
                        break;

                    case CODE_W_SCOPE:
                        writeMore128BytesTo(out, writeIntTo(out, in, tmp), in, tmp);
                        break;

                    case ARRAY:
                        writeMore128BytesTo(out, writeIntTo(out, in, tmp) - 4, in, tmp);
                        break;

                    case OBJECT:
                        writeMore128BytesTo(out, writeIntTo(out, in, tmp) - 4, in, tmp);
                        break;

                    case TIMESTAMP:
                        writeLess128BytesTo(out, 8, in, tmp);
                        break;

                    case MINKEY:
                        break;

                    case MAXKEY:
                        break;

                    default:
                        throw new UnsupportedOperationException("BSONDecoder doesn't understand type : " + type);
                }
            }

            if (size - 4 != out.size()) {
                logger.warn("Ops! Received wrong size of object. Expected " + size + " but actual is " + out.size());
            }
            return out.toByteArray();
        }

    }


    private Map<CString, CString> getPairNames() {

        int pos = currPosition;
        Map<CString, CString> namesMap = new HashMap<>();

        while (true) {

            final byte type = bytes[pos++];

            if (type == EOO) {
                break;
            }

            CString name = readCString(bytes, pos);

            // error detected! should be parsed in legacy mode
            // see com.mongodb.ServerError.getMsg() and
            //
            if (pos == 1 && (name.equals(err) || name.equals(err1) || name.equals(err2))) {
                return null;
            }

            namesMap.put(name, name);

            if (DEBUG)
                System.err.println("CString is [" + name + "] offset = " + name.getOffset() + " length = " + name.getLength());

            // skip CStr bytes
            pos += name.getLength() + 1;


            switch (type) {
                case NULL:
                    break;

                case UNDEFINED:
                    break;

                case BOOLEAN:
                    pos += 1;
                    break;

                case NUMBER:
                    pos += 8;
                    break;

                case NUMBER_INT:
                    if (DEBUG)
                        System.err.println("INT!");
                    pos += 4;
                    break;

                case NUMBER_LONG:
                    if (DEBUG)
                        System.err.println("LONG!!!! ]" + name + "[");
                    pos += 8;
                    break;

                case SYMBOL:
                    pos += readInt(bytes, pos) + 4;
                    break;

                case STRING:
                    if (DEBUG)
                        System.err.println("STRING!");
                    pos += readInt(bytes, pos) + 4;
                    break;

                case OID:
                    // ObjectId 3 * int
                    pos += 12;
                    break;

                case REF:
                    // length of ctring that follows
                    pos += readInt(bytes, pos) + 4;
                    // ObjectId 3 * int
                    pos += 12;
                    break;

                case DATE:
                    pos += 8;
                    break;

                case REGEX:
                    // todo: skip 2 CStr
//                        _callback.gotRegex(name, _in.readCStr(), _in.readCStr());
                    break;

                case BINARY:
                    if (DEBUG)
                        System.err.println("Binary size is " + readInt(bytes, pos));
                    pos += readInt(bytes, pos) + 4 + 1;
                    break;

                case CODE:
                    pos += readInt(bytes, pos) + 4;
                    break;

                case CODE_W_SCOPE:
                    pos += readInt(bytes, pos) + 4;
                    break;

                case ARRAY:
                    pos += readInt(bytes, pos);
                    break;

                case OBJECT:
                    pos += readInt(bytes, pos);
                    break;

                case TIMESTAMP:
                    pos += 8;
                    break;

                case MINKEY:
                    break;

                case MAXKEY:
                    break;

                default:
                    throw new UnsupportedOperationException("BSONDecoder doesn't understand type : " + type + " name: " + name);
            }
        }

        return namesMap;
    }

    public static final CString EMPTY_STRING = new CString("");

    static final CString[] ONE_BYTE_STRINGS = new CString[128];

    static void _fillRange(byte min, byte max) {
        while (min < max) {
            ONE_BYTE_STRINGS[(int) min] = new CString("" + (char) min);
            min++;
        }
    }

    static {
        _fillRange((byte) '0', (byte) '9');
        _fillRange((byte) 'a', (byte) 'z');
        _fillRange((byte) 'A', (byte) 'Z');
    }

    public CString readCString(byte[] bytes, final int startPos) {

        int pos = startPos;

        while ((bytes[pos++]) != 0) ;

        return new CString(bytes, startPos, pos - startPos - 1);
    }

    private void pushStack() {
        positionStack.push(currPosition);
        namesMapStack.push(currNamesMap);
    }

    private void popStack() {
        currPosition = positionStack.pop();
        currNamesMap = namesMapStack.pop();
    }


    /**
     * ============================= BDataInput methods =====================================
     */

    @Override
    public int readInt(CString name) {
        return readInt(name, 0);
    }

    @Override
    public int readInt(CString name, int defValue) {
        CString parsedName = currNamesMap.get(name);
        if (parsedName == null) {
            return defValue;
        }

        return BDecoderUtil.readInt(bytes, parsedName.getOffset() + parsedName.getLength() + 1);
    }

    @Override
    public int readInt(int i) {
        return readInt(CString.valueOf(i), 0);
    }

    @Override
    public int readInt(int i, int defValue) {
        return readInt(CString.valueOf(i), defValue);
    }

    @Override
    public long readLong(CString name) {
        return readLong(name, 0L);
    }

    @Override
    public long readLong(CString name, long defValue) {
        CString parsedName = currNamesMap.get(name);
        if (parsedName == null) {
            return defValue;
        }

        return BDecoderUtil.readLong(bytes, parsedName.getOffset() + parsedName.getLength() + 1);
    }

    @Override
    public long readLong(int i) {
        return readLong(CString.valueOf(i), 0L);
    }

    @Override
    public long readLong(int i, long defValue) {
        return readLong(CString.valueOf(i), defValue);
    }

    @Override
    public double readDouble(CString name) {
        return readDouble(name, 0D);
    }

    @Override
    public double readDouble(CString name, double defValue) {
        CString parsedName = currNamesMap.get(name);
        if (parsedName == null) {
            return defValue;
        }

        return BDecoderUtil.readDouble(bytes, parsedName.getOffset() + parsedName.getLength() + 1);
    }

    @Override
    public double readDouble(int i) {
        return readDouble(CString.valueOf(i), 0D);
    }

    @Override
    public double readDouble(int i, double defValue) {
        return readDouble(CString.valueOf(i), defValue);
    }

    @Override
    public String readString(CString name) {
        CString parsedName = currNamesMap.get(name);
        if (parsedName == null) {
            return null;
        }

        return BDecoderUtil.readString(bytes, parsedName.getOffset() + parsedName.getLength() + 1);
    }

    @Override
    public String readString(int i) {
        return readString(CString.valueOf(i));
    }

    @Override
    public Date readDate(CString name) {
        CString parsedName = currNamesMap.get(name);
        if (parsedName == null) {
            return null;
        }

        return new Date(BDecoderUtil.readLong(bytes, parsedName.getOffset() + parsedName.getLength() + 1));
    }

    @Override
    public Date readDate(int i) {
        return readDate(CString.valueOf(i));
    }

    @Override
    public UUID readUUID(CString name) {
        CString parsedName = currNamesMap.get(name);
        if (parsedName == null) {
            return null;
        }

        long part1 = BDecoderUtil.readLong(bytes, parsedName.getOffset() + parsedName.getLength() + 1 + 5);
        long part2 = BDecoderUtil.readLong(bytes, parsedName.getOffset() + parsedName.getLength() + 1 + 5 + 8);

        return new UUID(part1, part2);
    }

    @Override
    public UUID readUUID(int i) {
        return readUUID(CString.valueOf(i));
    }

    @Override
    public int readObject(CString name) {
        CString parsedName = currNamesMap.get(name);
        if (parsedName == null) {
            return -1;
        }

        pushStack();

        currPosition = parsedName.getOffset() + parsedName.getLength() + 1 + 4;
        currNamesMap = getPairNames();

        return currPosition;
    }

    @Override
    public int readObject(int i) {
        return readObject(CString.valueOf(i));
    }

    @Override
    public void readObjectStop(int label) {
        if (currPosition != label) {
            throw new IllegalStateException("Wrong decode sequence detected. expected label value = " + currPosition +
                    " actual = " + label);
        }
        popStack();
    }

    @Override
    public int readArray(CString name) {
        CString parsedName = currNamesMap.get(name);
        if (parsedName == null) {
            return -1;
        }

        pushStack();

        currPosition = parsedName.getOffset() + parsedName.getLength() + 1 + 4;
        currNamesMap = getPairNames();

        return currPosition;
    }

    @Override
    public int readArray(int i) {
        return readArray(CString.valueOf(i));
    }

    @Override
    public int readArraySize() {
        return currNamesMap.size();
    }


    @Override
    public void readArrayStop(int label) {
        if (currPosition != label) {
            throw new IllegalStateException("Wrong decode sequence detected. expected label value = " + currPosition +
                    " actual = " + label);
        }
        popStack();
    }


}
