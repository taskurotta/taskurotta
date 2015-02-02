package ru.taskurotta.mongodb.driver.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.mongodb.driver.CString;

import java.io.UnsupportedEncodingException;
import java.util.UUID;

/**
 */
public class BDecoderUtil {

    private static final Logger logger = LoggerFactory.getLogger(BDecoderUtil.class);

    public static final String DEFAULT_ENCODING = "UTF-8";


    public static long readLong(byte[] bytes, int offset) {
        return org.bson.io.Bits.readLong(bytes, offset);
    }

    public static int readInt(byte[] bytes, int offset) {
        return org.bson.io.Bits.readInt(bytes, offset);
    }

    public static String readString(byte[] bytes, int offset) {
        int size = org.bson.io.Bits.readInt(bytes, offset);
        try {
            return new String(bytes, offset + 4, size - 1, DEFAULT_ENCODING);
        } catch (UnsupportedEncodingException e) {
            logger.error("Can not create String", e);
            return null;
        }
    }

    public static UUID readUUID(CString name) {
        return null;
    }

    public int readObject(CString name) {
        return 0;
    }

    public void readObjectStop(int label) {

    }

    public int readArray(CString name) {
        return 0;
    }

    public int readArraySize() {
        return 0;
    }

    public long readLong(int i) {
        return 0;
    }

    public void readArrayStop(int label) {

    }

}
