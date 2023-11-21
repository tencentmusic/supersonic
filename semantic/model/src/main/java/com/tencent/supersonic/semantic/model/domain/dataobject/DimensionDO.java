package com.tencent.supersonic.semantic.model.domain.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.util.Date;

@Data
@TableName("s2_dimension")
public class DimensionDO {
    /**
     * 维度ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 主题域id
     */
    private Long modelId;

    /**
     * 所属数据源id
     */
    private Long datasourceId;

    /**
     * 维度名称
     */
    private String name;

    /**
     * 字段名称
     */
    private String bizName;

    /**
     * 描述
     */
    private String description;

    /**
     * 维度状态,0正常,1下架,2删除
     */
    private Integer status;

    /**
     * 敏感级别
     */
    private Integer sensitiveLevel;

    /**
     * 维度类型 categorical,time
     */
    private String type;

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
     * 语义类型DATE, ID, CATEGORY
     */
    private String semanticType;

    /**
     *
     */
    private String alias;

    /**
     * default values of dimension when query
     */
    private String defaultValues;

    /**
     *
     */
    private String dimValueMaps;

    /**
     * 类型参数
     */
    private String typeParams;

    /**
     * 表达式
     */
    private String expr;

    /**
     * 数据类型
     */
    private String dataType;

    private int isTag;
}