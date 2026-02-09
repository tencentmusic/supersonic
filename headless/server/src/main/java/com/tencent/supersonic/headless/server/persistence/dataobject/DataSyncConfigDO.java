package com.tencent.supersonic.headless.server.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("s2_data_sync_config")
public class DataSyncConfigDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;
    private Long sourceDatabaseId;
    private Long targetDatabaseId;
    private String syncConfig;
    private String cronExpression;
    private Integer retryCount;
    private Boolean enabled;
    private String quartzJobKey;
    private String createdBy;
    private Long tenantId;
    private Date createdAt;
    private Date updatedAt;
}
