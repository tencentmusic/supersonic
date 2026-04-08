package com.tencent.supersonic.headless.server.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("s2_business_topic")
public class BusinessTopicDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String description;
    private Integer priority;
    private Long ownerId;
    private String defaultDeliveryConfigIds;
    private Integer enabled;
    private Long tenantId;
    private String createdBy;
    private String updatedBy;
    private Date createdAt;
    private Date updatedAt;
}
