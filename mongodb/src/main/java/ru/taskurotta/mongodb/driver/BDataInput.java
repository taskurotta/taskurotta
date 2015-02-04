package ru.taskurotta.mongodb.driver;

import java.util.Date;
import java.util.UUID;

/**
 */
public interface BDataInput {

    public long readLong(CString name);

    public int readInt(CString name);

    public String readString(CString name);

    public Date readDate(CString name);

    public UUID readUUID(CString name);

    public int readObject(CString name);

    public void readObjectStop(int label);

    public int readArray(CString name);

    public int readArraySize();

    public long readLong(int i);

    public void readArrayStop(int label);

    public double readDouble(CString name);

    public double readDouble(int i);
}
