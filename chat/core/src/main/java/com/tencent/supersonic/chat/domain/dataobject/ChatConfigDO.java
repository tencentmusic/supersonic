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

    private String chatDetailConfig;

    private String chatAggConfig;

    private Integer status;

    /**
     * record info
     */
    private String createdBy;
    private String updatedBy;
    private Date createdAt;
    private Date updatedAt;

}