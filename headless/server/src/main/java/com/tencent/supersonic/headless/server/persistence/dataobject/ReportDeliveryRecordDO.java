package com.tencent.supersonic.headless.server.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("s2_report_delivery_record")
public class ReportDeliveryRecordDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String deliveryKey;
    private Long scheduleId;
    private Long executionId;
    private Long configId;
    private String deliveryType;
    private String status;
    private String fileLocation;
    private String errorMessage;
    private Integer retryCount;
    private Integer maxRetries;
    private Date nextRetryAt;
    private Date startedAt;
    private Date completedAt;
    private Long deliveryTimeMs;
    private Long tenantId;
    private Date createdAt;
}
