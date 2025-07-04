package com.tencent.supersonic.headless.api.pojo.request;

import com.tencent.supersonic.headless.api.pojo.SqlVariable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

@Data
public class SqlExecuteReq {
    public static final String LIMIT_WRAPPER = " SELECT * FROM ( %s ) a LIMIT %d ";

    @NotNull(message = "databaseId can not be null")
    private Long id;

    @NotBlank(message = "sql can not be blank")
    private String sql;

    private List<SqlVariable> sqlVariables;

    private Integer limit = 1000;

    public String getSql() {
        if(StringUtils.isNotBlank(sql)){
            sql=sql.replaceAll("^[\\n]+|[\\n]+$", "");
            sql=StringUtils.removeEnd(sql,";");
        }

        return String.format(LIMIT_WRAPPER, sql, limit);
    }
}
