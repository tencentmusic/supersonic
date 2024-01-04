package com.tencent.supersonic.chat.server.persistence.dataobject;

import java.util.Date;

import lombok.Data;
import lombok.ToString;


@Data
@ToString
public class ChatConfigDO {

    /**
     * database auto-increment primary key
     */
    private Long id;

    private Long modelId;

    private String chatDetailConfig;

    private String chatAggConfig;

    private String recommendedQuestions;

    private Integer status;

    private String llmExamples;

    /**
     * record info
     */
    private String createdBy;
    private String updatedBy;
    private Date createdAt;
    private Date updatedAt;

}