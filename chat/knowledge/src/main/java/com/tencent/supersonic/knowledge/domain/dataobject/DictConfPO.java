package com.tencent.supersonic.knowledge.domain.dataobject;

import java.util.Date;
import lombok.Data;

@Data
public class DictConfPO {

    private Long id;

    private Long domainId;

    private String dimValueInfos;

    private String createdBy;
    private String updatedBy;
    private Date createdAt;
    private Date updatedAt;

}