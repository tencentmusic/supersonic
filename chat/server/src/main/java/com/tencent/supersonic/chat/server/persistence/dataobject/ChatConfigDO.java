package com.tencent.supersonic.chat.server.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.ToString;

import java.util.Date;

@Data
@ToString
@TableName("s2_chat_config")
public class ChatConfigDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long modelId;

    private String chatDetailConfig;

    private String chatAggConfig;

    private String recommendedQuestions;

    private Integer status;

    private String llmExamples;

    /** record info */
    private String createdBy;

    private String updatedBy;
    private Date createdAt;
    private Date updatedAt;
}
