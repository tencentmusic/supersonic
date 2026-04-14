package com.tencent.supersonic.headless.server.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("s2_report_delivery_config")
public class ReportDeliveryConfigDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;
    private String deliveryType;
    private String deliveryConfig;
    private Boolean enabled;
    private String description;
    private Integer consecutiveFailures;
    private Integer maxConsecutiveFailures;
    private String disabledReason;
    private Long tenantId;
    private Date createdAt;
    private Date updatedAt;
    private String createdBy;
    private String updatedBy;
}
