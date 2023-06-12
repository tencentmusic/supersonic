package com.tencent.supersonic.semantic.api.core.request;


import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SqlExecuteReq {


    @NotNull(message = "domainId can not be null")
    private Long domainId;

    @NotBlank(message = "sql can not be blank")
    private String sql;

    public String getSql() {
        return String.format(" select * from ( %s ) a limit 1000 ", sql);
    }


}
