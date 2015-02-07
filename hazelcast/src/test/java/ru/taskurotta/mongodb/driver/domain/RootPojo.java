package ru.taskurotta.mongodb.driver.domain;

import java.util.Arrays;
import java.util.Date;
import java.util.UUID;

/**
 */
public class RootPojo {

    int i;
    String str;
    Date date;
    UUID uuid;
    long[] longArray = new long[] {10l, 4l, 2l, 4l};

    InnerPojo house = new InnerPojo();

    public RootPojo() {
    }

    public RootPojo(int i, String str, Date date, UUID uuid, String str2, UUID uuid2) {
        this.i = i;
        this.str = str;
        this.date = date;
        this.uuid = uuid;
        this.longArray = new long[] {10l, 4l, 2l, 4l};
        this.house = new InnerPojo(str2, uuid2);
    }

    public int getI() {
        return i;
    }

    public String getStr() {
        return str;
    }

    public Date getDate() {
        return date;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setI(int i) {
        this.i = i;
    }

    public void setStr(String str) {
        this.str = str;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public InnerPojo getHouse() {
        return house;
    }

    public void setHouse(InnerPojo house) {
        this.house = house;
    }

    public long[] getLongArray() {
        return longArray;
    }

    public void setLongArray(long[] longArray) {
        this.longArray = longArray;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RootPojo rootPojo = (RootPojo) o;

        if (date != null ? !date.equals(rootPojo.date) : rootPojo.date != null) {
            System.err.println("E DATE");
            return false;
        }
        if (house != null ? !house.equals(rootPojo.house) : rootPojo.house != null){
            System.err.println("E house");
            return false;
        }
        if (!Arrays.equals(longArray, rootPojo.longArray)) {
            System.err.println("E longArray");
            return false;
        }
        if (str != null ? !str.equals(rootPojo.str) : rootPojo.str != null) {
            System.err.println("E str");
            return false;
        }
        if (uuid != null ? !uuid.equals(rootPojo.uuid) : rootPojo.uuid != null) {
            System.err.println("E uuid");
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = i;
        result = 31 * result + (str != null ? str.hashCode() : 0);
        result = 31 * result + (date != null ? date.hashCode() : 0);
        result = 31 * result + (uuid != null ? uuid.hashCode() : 0);
        result = 31 * result + (longArray != null ? Arrays.hashCode(longArray) : 0);
        result = 31 * result + (house != null ? house.hashCode() : 0);
        return result;
    }
}