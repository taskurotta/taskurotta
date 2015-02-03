package ru.taskurotta.mongodb.driver;

import java.util.Date;
import java.util.UUID;

/**
 */
public interface BDataOutput {

    public void writeString(CString name, String value);

    public void writeString(int i, String value);

    public void writeUUID(CString name, UUID value);

    public void writeInt(CString name, int value);

    public void writeLong(CString name, long value);

    public void writeLong(int i, long value);

    public void writeDate(CString name, Date value);

    public int writeObject(CString name);

    public void writeObjectStop(int label);

    public int writeArray(CString name);

    public void writeArrayStop(int label);
}
