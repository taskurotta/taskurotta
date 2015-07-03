package ru.taskurotta.service.console.retriever.command;

import java.io.Serializable;

/**
 * Created on 15.06.2015.
 */
public class PageCommand implements Serializable {

    public static int DEFAULT_PAGE_NUM = 1;
    public static int DEFAULT_PAGE_SIZE = 10;

    protected int pageNum = DEFAULT_PAGE_NUM;
    protected int pageSize = DEFAULT_PAGE_SIZE;

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
