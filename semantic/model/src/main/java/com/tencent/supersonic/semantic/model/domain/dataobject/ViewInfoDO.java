package com.tencent.supersonic.semantic.model.domain.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.util.Date;

@Data
@TableName("s2_view_info")
public class ViewInfoDO {
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
