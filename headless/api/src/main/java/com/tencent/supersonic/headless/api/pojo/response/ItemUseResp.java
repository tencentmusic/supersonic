package com.tencent.supersonic.headless.api.pojo.response;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@ToString
public class ItemUseResp {

    private Long domainId;
    private String type;
    private Long itemId;
    private String bizName;
    private Long useCnt;

    public ItemUseResp(Long domainId, String type, String bizName, Long useCnt) {
        this.domainId = domainId;
        this.type = type;
        this.bizName = bizName;
        this.useCnt = useCnt;
    }
}