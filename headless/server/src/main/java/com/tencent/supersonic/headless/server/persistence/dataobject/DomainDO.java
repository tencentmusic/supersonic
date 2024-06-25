package com.tencent.supersonic.headless.server.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("s2_domain")
public class DomainDO {
    /**
     * 自增ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 主题域名称
     */
    private String name;

    /**
     * 内部名称
     */
    private String bizName;

    /**
     * 父主题域ID
     */
    private Long parentId;

    /**
     * 主题域状态
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
     * 主题域管理员
     */
    private String admin;

    /**
     * 主题域管理员组织
     */
    private String adminOrg;

    /**
     * 主题域是否公开
     */
    private Integer isOpen;

    /**
     * 主题域可用用户
     */
    private String viewer;

    /**
     * 主题域可用组织
     */
    private String viewOrg;

}
