package com.tencent.supersonic.common.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("s2_chat_model")
public class ChatModelDO {
    @TableId(type = IdType.AUTO)
    private Integer id;

    private String name;

    private String description;

    private String config;

    private Date createdAt;

    private String createdBy;

    private Date updatedAt;

    private String updatedBy;

    private String admin;

    private String viewer;

    private Integer isOpen;
}
