package com.tencent.supersonic.chat.server.persistence.dataobject;

import lombok.Data;

import java.util.Date;

@Data
public class DictConfDO {

    private Long id;

    private Long modelId;

    private String dimValueInfos;

    private String createdBy;
    private String updatedBy;
    private Date createdAt;
    private Date updatedAt;
}
