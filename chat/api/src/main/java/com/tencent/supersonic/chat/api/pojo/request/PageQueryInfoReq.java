package com.tencent.supersonic.chat.api.pojo.request;

import lombok.Data;

@Data
public class PageQueryInfoReq {

    private int current;

    private int pageSize;

    private String userName;

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public int getCurrent() {
        return current;
    }

    public void setCurrent(int current) {
        this.current = current;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }
}
