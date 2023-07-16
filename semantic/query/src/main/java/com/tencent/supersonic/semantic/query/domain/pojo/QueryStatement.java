package com.tencent.supersonic.semantic.query.domain.pojo;

import com.tencent.supersonic.semantic.api.core.response.SqlParserResp;
import lombok.Data;

@Data
public class QueryStatement {
    private Long domainId = 0L;
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
