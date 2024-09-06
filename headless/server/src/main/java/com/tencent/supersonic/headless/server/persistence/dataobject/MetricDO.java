package com.tencent.supersonic.headless.server.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("s2_metric")
public class MetricDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 主体域ID */
    private Long modelId;

    /** 指标名称 */
    private String name;

    /** 字段名称 */
    private String bizName;

    /** 描述 */
    private String description;

    /** 指标状态,0正常,1下架,2删除 */
    private Integer status;

    /** 敏感级别 */
    private Integer sensitiveLevel;

    /** 指标类型 proxy,expr */
    private String type;

    /** 创建时间 */
    private Date createdAt;

    /** 创建人 */
    private String createdBy;

    /** 更新时间 */
    private Date updatedAt;

    /** 更新人 */
    private String updatedBy;

    /** 数值类型 */
    private String dataFormatType;

    /** 数值类型参数 */
    private String dataFormat;

    /** */
    private String alias;

    /** */
    private String classifications;

    /** */
    private String relateDimensions;

    /** 类型参数 */
    private String typeParams;

    private String ext;

    private String defineType;

    private Integer isPublish;

    @TableField(exist = false)
    private int isTag;
}
