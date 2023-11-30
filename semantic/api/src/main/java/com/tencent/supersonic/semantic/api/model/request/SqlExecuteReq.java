package com.tencent.supersonic.semantic.api.model.request;


import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

@Data
public class SqlExecuteReq {
    public static final String LIMIT_WRAPPER = " select * from ( %s ) a limit 1000 ";

    @NotNull(message = "modelId can not be null")
    private Long id;

    @NotBlank(message = "sql can not be blank")
    private String sql;

    public String getSql() {
        if (StringUtils.isNotBlank(sql) && sql.endsWith(";")) {
            sql = sql.substring(0, sql.length() - 1);
        }
        return String.format(LIMIT_WRAPPER, sql);
    }

}
