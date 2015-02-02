package ru.taskurotta.mongodb.domain;

import java.util.Date;
import java.util.UUID;

/**
 */
public class RootPojo {

    int i;
    String str = "dsdsfdsf";
    Date date = new Date();
    UUID uuid = UUID.randomUUID();

    InnerPojo house = new InnerPojo();

    public RootPojo(int i) {
        this.i = i;
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
}