package com.tencent.supersonic.chat.server.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.tencent.supersonic.chat.api.pojo.enums.MemoryReviewResult;
import com.tencent.supersonic.chat.api.pojo.enums.MemoryStatus;
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
    private MemoryStatus status;

    @TableField("llm_review")
    private MemoryReviewResult llmReviewRet;

    @TableField("llm_comment")
    private String llmReviewCmt;

    @TableField("human_review")
    private MemoryReviewResult humanReviewRet;

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

}
