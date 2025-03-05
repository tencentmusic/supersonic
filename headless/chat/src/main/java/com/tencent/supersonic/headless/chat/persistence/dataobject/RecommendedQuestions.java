package com.tencent.supersonic.headless.chat.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.util.Date;

/**
 * 推荐问题配置表
 * 
 * @TableName recommended_questions
 */
@TableName("recommended_questions")
@Data
public class RecommendedQuestions {
    /**
     * 
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 助理ID
     */
    private Integer agentId;

    /**
     * 推荐问题
     */
    private String question;

    /**
     * 对应的物理SQL
     */
    @TableField("query_sql")
    private String querySql;

    /**
     * 启用状态: 1=启用, 0=禁用
     */
    private Integer status;

    /**
     * 创建时间
     */
    private Date createdAt;

    /**
     * 最后更新时间
     */
    private Date updatedAt;

}
