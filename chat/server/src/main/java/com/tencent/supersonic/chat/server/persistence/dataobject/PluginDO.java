package com.tencent.supersonic.chat.server.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.util.Date;

@Data
@TableName("s2_plugin")
public class PluginDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String type;

    private String dataSet;

    private String pattern;

    private String parseMode;

    private String name;

    private Date createdAt;

    private String createdBy;

    private Date updatedAt;

    private String updatedBy;

    private String parseModeConfig;

    private String config;

    private String comment;

}
