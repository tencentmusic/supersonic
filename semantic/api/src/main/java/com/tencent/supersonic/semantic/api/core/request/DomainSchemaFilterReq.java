package com.tencent.supersonic.semantic.api.core.request;

import java.util.List;
import lombok.Data;

@Data
public class DomainSchemaFilterReq {

    /**
     * if domainIds is empty, get all domain info
     */
    private List<Long> domainIds;
}