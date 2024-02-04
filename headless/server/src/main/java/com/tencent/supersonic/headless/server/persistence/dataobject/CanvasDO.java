package com.tencent.supersonic.headless.server.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("s2_canvas")
public class CanvasDO {
    /**
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     */
    private Long domainId;

    /**
     * datasource、dimension、metric
     */
    private String type;

    /**
     */
    private Date createdAt;

    /**
     */
    private String createdBy;

    /**
     */
    private Date updatedAt;

    /**
     */
    private String updatedBy;

    /**
     * config detail
     */
    private String config;
}
