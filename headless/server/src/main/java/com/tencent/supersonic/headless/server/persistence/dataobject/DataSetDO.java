package com.tencent.supersonic.headless.server.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("s2_data_set")
public class DataSetDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long domainId;

    private String name;

    private String bizName;

    private String description;

    private Integer status;

    private String alias;

    private String dataSetDetail;

    private Date createdAt;

    private String createdBy;

    private Date updatedAt;

    private String updatedBy;

    private String queryConfig;

    private String admin;

    private String adminOrg;
}
