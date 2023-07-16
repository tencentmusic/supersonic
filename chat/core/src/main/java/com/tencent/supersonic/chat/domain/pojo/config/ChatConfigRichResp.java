package com.tencent.supersonic.chat.domain.pojo.config;

import com.tencent.supersonic.common.enums.StatusEnum;
import java.util.Date;
import java.util.List;
import lombok.Data;

@Data
public class ChatConfigRichResp {

    private Long id;

    private Long domainId;

    private String domainName;
    private String bizName;

    private ChatAggRichConfig chatAggRichConfig;

    private ChatDetailRichConfig chatDetailRichConfig;

    /**
     * available status
     */
    private StatusEnum statusEnum;

    private String createdBy;
    private String updatedBy;
    private Date createdAt;
    private Date updatedAt;
}