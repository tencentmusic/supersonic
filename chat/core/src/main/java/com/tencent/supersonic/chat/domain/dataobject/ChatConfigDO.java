package com.tencent.supersonic.chat.domain.dataobject;

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

    private Long domainId;
    /**
     * default metrics information about the domain
     */
    private String defaultMetrics;

    /**
     * invisible dimensions/metrics
     */
    private String visibility;

    /**
     * the entity info about the domain
     */
    private String entity;

    /**
     * information about dictionary about the domain
     */
    private String knowledgeInfo;

    /**
     * available status
     */
    private Integer status;

    /**
     * record info
     */
    private String createdBy;
    private String updatedBy;
    private Date createdAt;
    private Date updatedAt;

}