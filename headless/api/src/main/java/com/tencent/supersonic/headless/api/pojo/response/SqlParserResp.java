package com.tencent.supersonic.headless.api.pojo.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class SqlParserResp {

    private String sql = "";
    private String sourceId = "";
    private String errMsg = "";
    private Boolean ok;

    public boolean isOk() {
        this.ok = "".equals(errMsg) && !"".equals(sql);
        return ok;
    }

    public SqlParserResp error(String msg) {
        this.setErrMsg(msg);
        return this;
    }
}
