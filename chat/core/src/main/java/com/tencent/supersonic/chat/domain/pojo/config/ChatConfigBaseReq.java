package com.tencent.supersonic.chat.domain.pojo.config;

import com.tencent.supersonic.common.enums.StatusEnum;

import java.util.List;

import lombok.Data;
import lombok.ToString;

/**
 * extended information command about domain
 */
@Data
@ToString
public class ChatConfigBaseReq {

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

}