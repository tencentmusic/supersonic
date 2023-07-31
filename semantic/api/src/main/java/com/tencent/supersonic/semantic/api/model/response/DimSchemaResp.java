package com.tencent.supersonic.semantic.api.model.response;

import lombok.Data;
import lombok.ToString;

@Data
@ToString(callSuper = true)
public class DimSchemaResp extends DimensionResp {

    private Long useCnt = 0L;

}