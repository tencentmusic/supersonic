package com.tencent.supersonic.headless.server.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("s2_alert_event")
public class AlertEventDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long executionId;
    private Long ruleId;
    private Integer conditionIndex;
    private String severity;
    private String alertKey;
    private String dimensionValue;
    private Double metricValue;
    private Double baselineValue;
    private Double deviationPct;
    private String message;
    private String deliveryStatus;
    private Date silenceUntil;

    // --- Resolution workflow ---
    private String resolutionStatus; // OPEN / CONFIRMED / ASSIGNED / RESOLVED / CLOSED
    private String acknowledgedBy;
    private Date acknowledgedAt;
    private Long assigneeId;
    private Date assignedAt;
    private String resolvedBy;
    private Date resolvedAt;
    private Date closedAt;
    private String notes;

    private Long tenantId;
    private Date createdAt;
}
