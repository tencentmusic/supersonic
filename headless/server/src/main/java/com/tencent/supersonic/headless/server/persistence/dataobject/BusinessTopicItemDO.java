package com.tencent.supersonic.headless.server.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("s2_business_topic_item")
public class BusinessTopicItemDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long topicId;
    private String itemType;
    private Long itemId;
    private Long tenantId;
}
