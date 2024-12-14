package com.tencent.supersonic.headless.api.pojo.response;

import lombok.Data;
import lombok.ToString;

@Data
@ToString(callSuper = true)
public class DimSchemaResp extends DimensionResp {

    private Long useCnt = 0L;

    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
