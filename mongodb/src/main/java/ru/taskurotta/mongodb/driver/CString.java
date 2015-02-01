package ru.taskurotta.mongodb.driver;

import org.bson.BSONException;
import org.bson.io.OutputBuffer;

import java.io.ByteArrayOutputStream;

/**
 */
public class CString {

    private String string;
    private byte[] bytes;

    public CString(String string) {
        this.string = string;
        prepareBytes();
    }

    private void prepareBytes() {

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
    }

    public void writeCString(OutputBuffer out) {

        out.write(bytes);
        out.write((byte) 0);
    }

}
