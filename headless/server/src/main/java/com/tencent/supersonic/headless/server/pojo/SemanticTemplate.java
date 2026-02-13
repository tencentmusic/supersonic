package com.tencent.supersonic.headless.server.pojo;

import lombok.Data;

import java.util.Date;

@Data
public class SemanticTemplate {

    private Long id;

    private String name;

    private String bizName;

    private String description;

    private String category;

    private SemanticTemplateConfig templateConfig;

    private String previewImage;

    private Long currentVersion;

    private Integer status;

    private Boolean isBuiltin;

    private Long tenantId;

    private Date createdAt;

    private String createdBy;

    private Date updatedAt;

    private String updatedBy;

    public boolean isBuiltinTemplate() {
        return isBuiltin != null && isBuiltin;
    }
}
