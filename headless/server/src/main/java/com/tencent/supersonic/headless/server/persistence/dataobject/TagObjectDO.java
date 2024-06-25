package com.tencent.supersonic.headless.server.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("s2_tag_object")
public class TagObjectDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 关联到某个主题域下
     */
    private Long domainId;

    /**
     * 标签对象名称
     */
    private String name;

    /**
     * 标签对象业务名称
     */
    private String bizName;

    /**
     * 描述
     */
    private String description;

    /**
     * 状态,0正常,1下架,2删除
     */
    private Integer status;

    /**
     * 敏感级别
     */
    private Integer sensitiveLevel;

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
     * 扩展信息
     */
    private String ext;
}