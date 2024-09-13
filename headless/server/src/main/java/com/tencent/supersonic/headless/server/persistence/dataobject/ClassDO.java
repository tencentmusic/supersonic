package com.tencent.supersonic.headless.server.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("s2_class")
public class ClassDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long domainId;
    private Long dataSetId;

    private String name;
    private String bizName;
    private String description;
    private Long parentId;

    /** 分类状态 */
    private Integer status;

    /** METRIC、DIMENSION、TAG */
    private String type;

    private String itemIds;

    private Date createdAt;
    private String createdBy;
    private Date updatedAt;
    private String updatedBy;
}
