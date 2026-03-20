package com.tencent.supersonic.headless.server.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("s2_alert_execution")
public class AlertExecutionDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long ruleId;
    private String status;
    private Date startTime;
    private Date endTime;
    private Long totalRows;
    private Long alertedRows;
    private Long silencedRows;
    private String errorMessage;
    private Long executionTimeMs;
    private Long tenantId;
    private Date createdAt;
}
