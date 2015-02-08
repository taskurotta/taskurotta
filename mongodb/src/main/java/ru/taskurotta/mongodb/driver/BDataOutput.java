package ru.taskurotta.mongodb.driver;

import java.util.Date;
import java.util.UUID;

/**
 */
public interface BDataOutput {

    public void writeInt(CString name, int value);

    public void writeInt(int i, int value);

    public void writeLong(CString name, long value);

    public void writeLong(int i, long value);

    public void writeDouble(CString name, double value);

    public void writeDouble(int i, double value);

    public void writeBoolean(CString name, boolean value);

    public void writeBoolean(int i, boolean value);

    public void writeString(CString name, String value);

    public void writeString(int i, String value);

    public void writeUUID(CString name, UUID value);

    public void writeUUID(int i, UUID value);

    public void writeDate(CString name, Date value);

    public void writeDate(int i, Date value);

    public int writeObject(CString name);

    public int writeObject(int i);

    public void writeObjectStop(int label);

    public int writeArray(CString name);

    public int writeArray(int i);

    public void writeArrayStop(int label);
}
