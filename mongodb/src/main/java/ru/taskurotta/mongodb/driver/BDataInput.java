package ru.taskurotta.mongodb.driver;

import java.util.Date;
import java.util.Set;
import java.util.UUID;

/**
 */
public interface BDataInput {

    public int readInt(CString name);

    public int readInt(CString name, int defValue);

    public int readInt(int i);

    public int readInt(int i, int defValue);

    public long readLong(CString name);

    public long readLong(CString name, long defValue);

    public long readLong(int i);

    public long readLong(int i, long defValue);

    public double readDouble(CString name);

    public double readDouble(CString name, double defValue);

    public double readDouble(int i);

    public double readDouble(int i, double defValue);

    public boolean readBoolean(CString name);

    public boolean readBoolean(CString name, boolean defValue);

    public boolean readBoolean(int i);

    public boolean readBoolean(int i, boolean defValue);

    public String readString(CString name);

    public String readString(int i);

    public Date readDate(CString name);

    public Date readDate(int i);

    public UUID readUUID(CString name);

    public UUID readUUID(int i);

    public int readObject(CString name);

    public int readObject(int i);

    public void readObjectStop(int label);

    public int readArray(CString name);

    public int readArray(int i);

    public int readArraySize();

    public void readArrayStop(int label);

    public Set<CString> readPairNames();

}
