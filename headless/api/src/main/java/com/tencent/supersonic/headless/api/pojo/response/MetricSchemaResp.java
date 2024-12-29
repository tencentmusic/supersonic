package com.tencent.supersonic.headless.api.pojo.response;

import com.google.common.collect.Sets;
import lombok.Data;
import lombok.ToString;

import java.util.Set;

@Data
@ToString(callSuper = true)
public class MetricSchemaResp extends MetricResp {

    private Long useCnt = 0L;
    private Set<String> fields = Sets.newHashSet();

    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

}
