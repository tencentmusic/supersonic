package com.tencent.supersonic.headless.server.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("s2_dictionary_task")
public class DictTaskDO {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;
    private String description;
    private String type;
    private Long itemId;
    private String config;
    private String status;
    private Date createdAt;
    private String createdBy;
    private Long elapsedMs;
}
