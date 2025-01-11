package com.tencent.supersonic.headless.server.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("s2_query_stat_info")
public class QueryStatDO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String traceId;
    private Long modelId;
    private Long dataSetId;
    private String queryUser;
    private String createdAt;
    /** corresponding type, such as sql, struct, etc */
    private String queryType;
    /** NORMAL, PRE_FLUSH */
    private Integer queryTypeBack;
    private String querySqlCmd;
    @TableField("sql_cmd_md5")
    private String querySqlCmdMd5;
    private String queryStructCmd;
    @TableField("struct_cmd_md5")
    private String queryStructCmdMd5;
    private String querySql;
    private String sqlMd5;
    private String queryEngine;
    // private Long startTime;
    private Long elapsedMs;
    private String queryState;
    private Boolean nativeQuery;
    private String startDate;
    private String endDate;
    private String dimensions;
    private String metrics;
    private String selectCols;
    private String aggCols;
    private String filterCols;
    private String groupByCols;
    private String orderByCols;
    private Boolean useResultCache;
    private Boolean useSqlCache;
    private String sqlCacheKey;
    private String resultCacheKey;
    private String queryOptMode;
}
