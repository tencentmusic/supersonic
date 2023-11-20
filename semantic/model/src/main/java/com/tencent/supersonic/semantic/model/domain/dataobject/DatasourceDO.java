package com.tencent.supersonic.semantic.model.domain.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.util.Date;

@Data
@TableName("s2_datasource")
public class DatasourceDO {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     *  模型ID
     */
    private Long modelId;

    /**
     * 数据源名称
     */
    private String name;

    /**
     * 内部名称
     */
    private String bizName;

    /**
     * 数据源描述
     */
    private String description;

    /**
     * 数据库实例ID
     */
    private Long databaseId;

    /**
     * 
     */
    private Integer status;

    /**
     * 创建时间
     */
    private Date createdAt;

    /**
     * 创建人
     */
    private String createdBy;

    /**
     * 更新时间
     */
    private Date updatedAt;

    /**
     * 更新人
     */
    private String updatedBy;

    /**
     * 数据源配置
     */
    private String datasourceDetail;

    /**
     * 上游依赖标识
     */
    private String depends;

    private String filterSql;

}