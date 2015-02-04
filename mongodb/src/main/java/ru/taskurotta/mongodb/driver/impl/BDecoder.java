package ru.taskurotta.mongodb.driver.impl;

import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.DefaultDBDecoder;
import ru.taskurotta.mongodb.driver.BDataInput;
import ru.taskurotta.mongodb.driver.CString;
import ru.taskurotta.mongodb.driver.DBObjectСheat;
import ru.taskurotta.mongodb.driver.StreamBSerializer;

import java.io.IOException;
import java.io.InputStream;
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
            return new DBObjectСheat(rootObj);
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
        boolean isError = decodeInternal(readDocumentByteArray(in));

        if (!isError) {
            return super.decode(in, collection);
        }

        try {
            return new DBObjectСheat(rootObj);
        } finally {
            clear();
            // todo: put decoder back to object poll
        }
    }

    public static byte[] readDocumentByteArray(InputStream in) throws IOException {
        byte[] bytes4Int = new byte[4];
        in.read(bytes4Int);

        byte[] allStreamInBytes = new byte[readInt(bytes4Int, 0) - 4];
        in.read(allStreamInBytes);

        return allStreamInBytes;
    }

    public static int readInt(byte[] data, int offset) {
        int x = 0;
        x |= (0xFF & data[offset + 0]) << 0;
        x |= (0xFF & data[offset + 1]) << 8;
        x |= (0xFF & data[offset + 2]) << 16;
        x |= (0xFF & data[offset + 3]) << 24;
        return x;
    }

    private boolean decodeInternal(byte[] bytes) {

        if (bytes == null) {
            throw new IllegalStateException("not ready");
        }

        this.bytes = bytes;
        currPosition = 0;
        currNamesMap = getPairNames();

        if (currNamesMap == null) {
            return false;
        }

        rootObj = streamBSerializer.read(this);
        return true;
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
    public long readLong(CString name) {
        CString parsedName = currNamesMap.get(name);
        if (parsedName == null) {
            return 0L;
        }

        return BDecoderUtil.readLong(bytes, parsedName.getOffset() + parsedName.getLength() + 1);
    }

    @Override
    public int readInt(CString name) {
        CString parsedName = currNamesMap.get(name);
        if (parsedName == null) {
            return 0;
        }

        return BDecoderUtil.readInt(bytes, parsedName.getOffset() + parsedName.getLength() + 1);
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
    public Date readDate(CString name) {
        CString parsedName = currNamesMap.get(name);
        if (parsedName == null) {
            return null;
        }

        return new Date(BDecoderUtil.readLong(bytes, parsedName.getOffset() + parsedName.getLength() + 1));
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
    public int readArraySize() {
        return currNamesMap.size();
    }

    @Override
    public long readLong(int i) {
        CString id;

        if (i > -1 && i < BEncoder.ID_CACHE_SIZE) {
            id = BEncoder.ARRAY_INDEXES[i];
        } else {
            id = new CString(i);
        }

        return readLong(id);
    }

    @Override
    public void readArrayStop(int label) {
        if (currPosition != label) {
            throw new IllegalStateException("Wrong decode sequence detected. expected label value = " + currPosition +
                    " actual = " + label);
        }
        popStack();
    }

    @Override
    public double readDouble(CString name) {
        CString parsedName = currNamesMap.get(name);
        if (parsedName == null) {
            return 0D;
        }

        return BDecoderUtil.readDouble(bytes, parsedName.getOffset() + parsedName.getLength() + 1);
    }

    @Override
    public double readDouble(int i) {
        CString id;

        if (i > -1 && i < BEncoder.ID_CACHE_SIZE) {
            id = BEncoder.ARRAY_INDEXES[i];
        } else {
            id = new CString(i);
        }

        return readDouble(id);
    }

}
