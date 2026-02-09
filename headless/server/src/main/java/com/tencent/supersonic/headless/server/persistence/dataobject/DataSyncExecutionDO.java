package com.tencent.supersonic.headless.server.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("s2_data_sync_execution")
public class DataSyncExecutionDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long syncConfigId;
    private Long connectionId;
    private String jobType;
    private Integer attemptNumber;
    private String status;
    private Date startTime;
    private Date endTime;
    private Long rowsRead;
    private Long rowsWritten;
    private Long bytesSynced;
    private String watermarkValue;
    private String errorMessage;
    private Long tenantId;
}
