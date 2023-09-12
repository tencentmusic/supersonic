package com.tencent.supersonic.semantic.query.persistence.pojo;

import lombok.Data;

@Data
public class QueryStatement {
    private Long modelId = 0L;
    private String sql = "";
    private String sourceId = "";
    private String errMsg = "";
    private Boolean ok;

    public boolean isOk() {
        this.ok = "".equals(errMsg) && !"".equals(sql);
        return ok;
    }

    public QueryStatement error(String msg) {
        this.setErrMsg(msg);
        return this;
    }
}
