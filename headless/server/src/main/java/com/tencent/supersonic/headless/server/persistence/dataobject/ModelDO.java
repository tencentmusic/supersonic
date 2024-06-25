package com.tencent.supersonic.headless.server.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("s2_model")
public class ModelDO {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long domainId;

    private Long tagObjectId;

    private String name;

    private String bizName;

    private String description;

    private Long databaseId;

    private Integer status;

    private Date createdAt;

    private String createdBy;

    private Date updatedAt;

    private String updatedBy;

    private String modelDetail;

    private String depends;

    private String filterSql;

    private String viewer;

    private String viewOrg;

    private String admin;

    private String adminOrg;

    private Integer isOpen;

    private String drillDownDimensions;

    private String alias;

    private String sourceType;

    private String ext;

}
