package com.dxy.apache.dbutils.result;

import java.util.ArrayList;
import java.util.List;

/**
 * 分页数据的返回dto
 * @author dxy
 * 2018/4/24 20:15
 */
public class Pagination<T> {

    private long totalCount = 0;
    private int pageSize = 15;
    private int pageNo = 1;

    /**
     * 当前页的数据
     */
    private List<T> list = new ArrayList<>();

    public Pagination() {
    }

    public Pagination(int pageNo, int pageSize, long totalCount) {
        if (totalCount <= 0) {
            this.totalCount = 0;
        } else {
            this.totalCount = totalCount;
        }
        if (pageSize <= 0) {
            //默认单页数据条数为15
            this.pageSize = 15;
        } else {
            this.pageSize = pageSize;
        }
        if (pageNo <= 0) {
            this.pageNo = 1;
        } else {
            this.pageNo = pageNo;
        }
    }

    public Pagination(int pageNo, int pageSize, List<T> list) {
        this(pageNo, pageSize, 9999, list);
    }

    public Pagination(int pageNo, int pageSize, long totalCount, List<T> list) {
        this(pageNo, pageSize, totalCount);
        this.list = list;
    }

    public long getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(long totalCount) {
        this.totalCount = totalCount;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public int getPageNo() {
        return pageNo;
    }

    public void setPageNo(int pageNo) {
        this.pageNo = pageNo;
    }

    public List<T> getList() {
        return list;
    }

    public void setList(List<T> list) {
        this.list = list;
    }
}

