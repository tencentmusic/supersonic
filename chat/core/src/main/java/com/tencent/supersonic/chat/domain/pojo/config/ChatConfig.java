package com.tencent.supersonic.chat.domain.pojo.config;

import com.tencent.supersonic.common.enums.StatusEnum;
import com.tencent.supersonic.common.util.RecordInfo;
import java.util.List;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class ChatConfig {

    /**
     * database auto-increment primary key
     */
    private Long id;

    private Long domainId;

    /**
     * the chatDetailConfig about the domain
     */
    private ChatDetailConfig chatDetailConfig;

    /**
     * the chatAggConfig about the domain
     */
    private ChatAggConfig chatAggConfig;

    /**
     * available status
     */
    private StatusEnum status;

    /**
     * about createdBy, createdAt, updatedBy, updatedAt
     */
    private RecordInfo recordInfo;

}