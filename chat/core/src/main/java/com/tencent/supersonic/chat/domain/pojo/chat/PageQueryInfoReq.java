package com.tencent.supersonic.chat.domain.pojo.chat;

import lombok.Data;

@Data
public class PageQueryInfoReq {

    private int current;

    private int pageSize;

    private String userName;

    public int getPageSize() {
        return pageSize;
    }

    public int getCurrent() {
        return current;
    }

    public String getUserName() {
        return userName;
    }

    public void setCurrent(int current) {
        this.current = current;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }
}
