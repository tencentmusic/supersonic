package com.tencent.supersonic.common.pojo;

import lombok.Data;

@Data
public class PageBaseReq {

    private static final Integer MAX_PAGESIZE = 100;
    private Integer current = 1;
    private Integer pageSize = 10;
    private String sort = "desc";
    private String orderCondition;

    public Integer getLimitStart() {
        return this.pageSize * (this.current - 1);
    }

    public Integer getLimitSize() {
        return this.pageSize;
    }

}
