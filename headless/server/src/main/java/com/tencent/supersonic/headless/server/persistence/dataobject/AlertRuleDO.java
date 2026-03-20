package com.tencent.supersonic.headless.server.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("s2_alert_rule")
public class AlertRuleDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;
    private String description;
    private Long datasetId;
    private String queryConfig;
    private String conditions;
    private String cronExpression;
    private Integer enabled;
    private Long ownerId;
    private String deliveryConfigIds;
    private Integer silenceMinutes;
    private Integer maxConsecutiveFailures;
    private Integer consecutiveFailures;
    private String disabledReason;
    private Integer retryCount;
    private Integer retryInterval;
    private String quartzJobKey;
    private Date lastCheckTime;
    private Date nextCheckTime;
    private Date createdAt;
    private Date updatedAt;
    private String createdBy;
    private Long tenantId;
}
