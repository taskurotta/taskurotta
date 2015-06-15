package ru.taskurotta.service.console.retriever.command;

import java.io.Serializable;

/**
 * Created on 15.06.2015.
 */
public class PageCommand implements Serializable {
    protected int pageNum;
    protected int pageSize;

    public int getPageNum() {
        return pageNum;
    }

    public void setPageNum(int pageNum) {
        this.pageNum = pageNum;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    @Override
    public String toString() {
        return "PageCommand{" +
                "pageNum=" + pageNum +
                ", pageSize=" + pageSize +
                '}';
    }
}
