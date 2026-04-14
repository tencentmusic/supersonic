package com.tencent.supersonic.headless.api.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("s2_report_execution")
public class ReportExecutionDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long scheduleId;
    private Integer attempt;
    private String status;
    private Date startTime;
    private Date endTime;
    private String resultLocation;
    private String errorMessage;
    private Long rowCount;
    private String sqlHash;
    private Long tenantId;
    private String executionSnapshot;
    private Long templateVersion;
    private String engineVersion;
    private Long scanRows;
    private Long executionTimeMs;
    private Long ioBytes;
}
