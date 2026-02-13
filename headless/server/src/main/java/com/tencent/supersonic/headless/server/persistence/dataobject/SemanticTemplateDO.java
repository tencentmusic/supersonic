package com.tencent.supersonic.headless.server.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("s2_semantic_template")
public class SemanticTemplateDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String bizName;

    private String description;

    private String category;

    private String templateConfig;

    private String previewImage;

    private Long currentVersion;

    private Integer status;

    private Integer isBuiltin;

    private Long tenantId;

    private Date createdAt;

    private String createdBy;

    private Date updatedAt;

    private String updatedBy;
}
