package com.tencent.supersonic.headless.server.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("s2_semantic_deployment")
public class SemanticDeploymentDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long templateId;

    private String templateName;

    private Long databaseId;

    private String paramConfig;

    private String status;

    private String resultDetail;

    private String errorMessage;

    private String currentStep;

    private Date startTime;

    private Date endTime;

    private Long tenantId;

    private Date createdAt;

    private String createdBy;

    /**
     * Non-null when status is PENDING/RUNNING, value = "{templateId}_{tenantId}". NULL otherwise.
     */
    private String activeLock;
}
