package ru.taskurotta.backend.ora.tools;

/**
* User: greg
*/
public class SqlParam {
    private int index;
    private String stringParam;
    private long longParam = -1;

    public SqlParam(int index, String stringParam) {
        this.index = index;
        this.stringParam = stringParam;
    }

    public SqlParam(int index, long longParam) {
        this.index = index;
        this.longParam = longParam;
    }

    public int getIndex() {
        return index;
    }

    public String getStringParam() {
        return stringParam;
    }

    public long getLongParam() {
        return longParam;
    }

}
