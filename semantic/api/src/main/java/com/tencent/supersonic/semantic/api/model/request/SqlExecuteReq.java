package com.tencent.supersonic.semantic.api.model.request;


import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SqlExecuteReq {
    public static final String LIMIT_WRAPPER = " select * from ( %s ) a limit 1000 ";

    @NotNull(message = "modelId can not be null")
    private Long id;

    @NotBlank(message = "sql can not be blank")
    private String sql;

    public String getSql() {
        return String.format(LIMIT_WRAPPER, sql);
    }


}
