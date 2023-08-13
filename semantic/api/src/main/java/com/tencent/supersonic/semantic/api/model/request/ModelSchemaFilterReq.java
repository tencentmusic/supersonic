package com.tencent.supersonic.semantic.api.model.request;

import java.util.List;
import lombok.Data;

@Data
public class ModelSchemaFilterReq {

    /**
     * if domainIds is empty, get all domain info
     */
    private List<Long> modelIds;
}