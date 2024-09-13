package com.tencent.supersonic.headless.server.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("s2_app")
public class AppDO {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private String name;

    private String description;

    private String config;

    private Date endDate;

    private Integer qps;

    private String owner;

    private Integer status;

    private String appSecret;

    private String createdBy;

    private String updatedBy;

    private Date createdAt;

    private Date updatedAt;
}
