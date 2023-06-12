package com.tencent.supersonic.semantic.api.core.response;

import java.util.Date;
import lombok.Data;

@Data
public class DatasourceRelaResp {

    private Long id;

    private Long domainId;

    private Long datasourceFrom;

    private Long datasourceTo;

    private String joinKey;

    private Date createdAt;

    private String createdBy;

    private Date updatedAt;

    private String updatedBy;

}