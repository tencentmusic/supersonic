package com.tencent.supersonic.headless.server.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

@Data
@TableName("s2_tag")
public class TagDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 主体域ID
     */
    private Long modelId;

    /**
     * 指标名称
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
     * 指标状态,0正常,1下架,2删除
     */
    private Integer status;

    /**
     * 敏感级别
     */
    private Integer sensitiveLevel;

    /**
     * 类型 DERIVED,ATOMIC
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
     * 类型参数
     */
    private String defineType;
    private String typeParams;
    private String ext;

}
