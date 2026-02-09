package com.tencent.supersonic.headless.server.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("s2_connection")
public class ConnectionDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;
    private String description;
    private Long sourceDatabaseId;
    private Long destinationDatabaseId;

    // Lifecycle status
    private String status;
    private Date statusUpdatedAt;
    private String statusMessage;

    // Schema configuration (Airbyte catalog format)
    private String configuredCatalog;
    private String discoveredCatalog;
    private Date discoveredCatalogAt;
    private String schemaChangeStatus;
    private String schemaChangeDetail;

    // Schedule configuration
    private String scheduleType;
    private String cronExpression;
    private Integer scheduleUnits;
    private String scheduleTimeUnit;

    // Checkpointing
    private String state;
    private String stateType;

    // Retry configuration
    private Integer retryCount;
    private String advancedConfig;

    // Quartz integration
    private String quartzJobKey;

    // Audit fields
    private String createdBy;
    private String updatedBy;
    private Long tenantId;
    private Date createdAt;
    private Date updatedAt;
}
