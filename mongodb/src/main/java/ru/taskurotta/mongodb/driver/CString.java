package ru.taskurotta.mongodb.driver;

import org.bson.BSONException;
import org.bson.io.OutputBuffer;
import ru.taskurotta.mongodb.driver.impl.BDecoderUtil;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;

/**
 */
public class CString {

    protected byte[] bytes;
    protected int offset = 0;
    protected int length;

    int hashCode = 0;
    boolean hashCached = false;

    public CString(int i) {
        prepareBytes(Integer.toString(i));
    }

    public CString(String string) {
        prepareBytes(string);
    }

    public CString(byte[] bytes, int offset, int length) {
        this.bytes = bytes;
        this.offset = offset;
        this.length = length;
    }

    private void prepareBytes(String string) {

        final int len = string.length();
        final ByteArrayOutputStream out = new ByteArrayOutputStream(len);

        for (int i = 0; i < len;/*i gets incremented*/) {
            final int c = Character.codePointAt(string, i);

            if (c == 0x0) {
                throw new BSONException(
                        String.format("BSON cstring '%s' is not valid because it contains a null character at index " +
                                "%d", string, i));
            }
            if (c < 0x80) {
                out.write((byte) c);
            } else if (c < 0x800) {
                out.write((byte) (0xc0 + (c >> 6)));
                out.write((byte) (0x80 + (c & 0x3f)));
            } else if (c < 0x10000) {
                out.write((byte) (0xe0 + (c >> 12)));
                out.write((byte) (0x80 + ((c >> 6) & 0x3f)));
                out.write((byte) (0x80 + (c & 0x3f)));
            } else {
                out.write((byte) (0xf0 + (c >> 18)));
                out.write((byte) (0x80 + ((c >> 12) & 0x3f)));
                out.write((byte) (0x80 + ((c >> 6) & 0x3f)));
                out.write((byte) (0x80 + (c & 0x3f)));
            }

            i += Character.charCount(c);
        }

        bytes = out.toByteArray();

        length = bytes.length;
    }

    public void writeCString(OutputBuffer out) {

        out.write(bytes);
        out.write((byte) 0);
    }

    @Override
    public String toString() {

        boolean isAscii = true;

        final int max = offset + length;
        for (int i = offset; i < max; i++) {
            isAscii = isAscii && _isAscii(bytes[i]);
        }

        if (isAscii) {
            return new String(bytes, offset, length);
        } else {
            try {
                return new String(bytes, offset, length, BDecoderUtil.DEFAULT_ENCODING);
            } catch (UnsupportedEncodingException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    public int getOffset() {
        return offset;
    }

    public int getLength() {
        return length;
    }


    private static boolean _isAscii(byte b) {
        return b >= 0 && b <= 127;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CString cString = (CString) o;

        int thisPos = offset;
        int thatPos = cString.offset;

        int max = offset + length;
        for (int i = offset; i < max; i++) {
            if (bytes[thisPos++] != cString.bytes[thatPos++]) {
                return false;
            }
        }

        return true;
    }

    @Override
    public int hashCode() {

        if (hashCached) {
            return hashCode;
        }

        int result = 1;

        int max = offset + length;
        for (int i = offset; i < max; i++) {
            result = 31 * result + bytes[i];
        }

        hashCode = result;
        hashCached = true;

        return result;
    }
}
