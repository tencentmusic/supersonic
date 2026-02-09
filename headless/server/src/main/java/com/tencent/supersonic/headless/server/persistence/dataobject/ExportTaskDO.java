package com.tencent.supersonic.headless.server.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("s2_export_task")
public class ExportTaskDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String taskName;
    private Long userId;
    private Long datasetId;
    private String queryConfig;
    private String outputFormat;
    private String status;
    private String fileLocation;
    private Long fileSize;
    private Long rowCount;
    private String errorMessage;
    private Date createdAt;
    private Date expireTime;
    private Long tenantId;
}
