package com.tencent.supersonic.headless.server.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("s2_term")
public class TermDO {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long domainId;

    private String name;

    private String description;

    private String alias;

    private String relatedMetrics;

    private String relatedDimensions;

    private Date createdAt;

    private String createdBy;

    private Date updatedAt;

    private String updatedBy;
}
