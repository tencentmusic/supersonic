package com.tencent.supersonic.chat.domain.pojo.config;

import com.tencent.supersonic.common.enums.StatusEnum;
import java.util.Date;
import lombok.Data;

@Data
public class ChatConfigResp {

    private Long id;

    private Long domainId;

    private ChatDetailConfig chatDetailConfig;

    private ChatAggConfig chatAggConfig;

    /**
     * available status
     */
    private StatusEnum statusEnum;

    private String createdBy;
    private String updatedBy;
    private Date createdAt;
    private Date updatedAt;
}