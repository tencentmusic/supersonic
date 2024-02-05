package com.tencent.supersonic.headless.server.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("s2_dictionary_conf")
public class DictConfDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String description;
    private String type;
    private Long itemId;
    private String config;
    private String status;
    private Date createdAt;
    private String createdBy;
}