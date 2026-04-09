package com.springbootexample.dto;

import java.util.List;

public class PaginationResponse<T> {

    private List<T> content;
    private int pageNumber;
    private int pageSize;

    public PaginationResponse(List<T> content, int pageNumber, int pageSize) {
        this.content = content;
        this.pageNumber = pageNumber;
        this.pageSize = pageSize;
    }

    public List<T> getContent() {
        return content;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public int getPageSize() {
        return pageSize;
    }
}