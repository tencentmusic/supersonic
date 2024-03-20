package com.tencent.supersonic.headless.server.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 收藏项表
 * </p>
 *
 * @author yannsu
 * @since 2023-11-09 03:49:33
 */
@Getter
@Setter
@TableName("s2_collect")
public class CollectDO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 收藏项ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 收藏的类型 metric,dimension,tag
     */
    @TableField("type")
    private String type;

    /**
     * 收藏人
     */
    @TableField("username")
    private String username;

    /**
     * 收藏ID
     */
    @TableField("collect_id")
    private Long collectId;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
