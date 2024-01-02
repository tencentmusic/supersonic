package com.tencent.supersonic.headless.common.core.pojo;

import lombok.Data;

@Data
public class Cache {

    private Boolean cache = true;

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("{");
        sb.append("\"cache\":")
                .append(cache);
        sb.append('}');
        return sb.toString();
    }
}