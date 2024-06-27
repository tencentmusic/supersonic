package com.tencent.supersonic.chat.server.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;

import java.util.Date;

@Data
@Builder
@ToString
@TableName("s2_chat_memory")
public class ChatMemoryDO {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("question")
    private String question;

    @TableField("agent_id")
    private Integer agentId;

    @TableField("db_schema")
    private String dbSchema;

    @TableField("s2_sql")
    private String s2sql;

    @TableField("status")
    private Status status;

    @TableField("llm_review")
    private ReviewResult llmReviewRet;

    @TableField("llm_comment")
    private String llmReviewCmt;

    @TableField("human_review")
    private ReviewResult humanReviewRet;

    @TableField("human_comment")
    private String humanReviewCmt;

    @TableField("created_by")
    private String createdBy;

    @TableField("created_at")
    private Date createdAt;

    @TableField("updated_by")
    private String updatedBy;

    @TableField("updated_at")
    private Date updatedAt;

    public enum ReviewResult {
        POSITIVE,
        NEGATIVE
    }

    public enum Status {
        PENDING,
        ENABLED,
        DISABLED;
    }
}
