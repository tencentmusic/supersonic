package com.tencent.supersonic.headless.server.persistence.dataobject;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("s2_metric_query_default_config")
public class MetricQueryDefaultConfigDO {


    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long metricId;

    private String userName;

    private String defaultConfig;

    private String appKey;

    private String appSecret;

    private String owner;

    private Date createdAt;

    private String createdBy;

    private Date updatedAt;

    private String updatedBy;

}
