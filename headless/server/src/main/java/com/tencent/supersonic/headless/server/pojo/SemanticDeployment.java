package com.tencent.supersonic.headless.server.pojo;

import com.tencent.supersonic.headless.api.pojo.SemanticDeployResult;
import com.tencent.supersonic.headless.api.pojo.SemanticTemplateConfig;
import lombok.Data;

import java.util.Date;

@Data
public class SemanticDeployment {

    private Long id;

    private Long templateId;

    private String templateName;

    private Long templateVersion;

    private SemanticTemplateConfig templateConfigSnapshot;

    private Long databaseId;

    private SemanticDeployParam paramConfig;

    private DeploymentStatus status;

    private SemanticDeployResult resultDetail;

    private String errorMessage;

    private String currentStep;

    private Date startTime;

    private Date endTime;

    private Long tenantId;

    private Date createdAt;

    private String createdBy;

    public enum DeploymentStatus {
        PENDING, RUNNING, SUCCESS, FAILED, CANCELLED
    }
}
