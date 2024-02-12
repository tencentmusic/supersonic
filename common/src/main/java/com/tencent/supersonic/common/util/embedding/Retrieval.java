package com.tencent.supersonic.common.util.embedding;

import com.google.common.base.Objects;
import com.tencent.supersonic.common.pojo.Constants;
import java.util.Map;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

@Data
public class Retrieval {

    protected String id;

    protected double distance;

    protected String query;

    protected Map<String, Object> metadata;

    public static Long getLongId(Object id) {
        if (id == null || StringUtils.isBlank(id.toString())) {
            return null;
        }
        String[] split = id.toString().split(Constants.UNDERLINE);
        return Long.parseLong(split[0]);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Retrieval retrieval = (Retrieval) o;
        return Double.compare(retrieval.distance, distance) == 0 && Objects.equal(id,
                retrieval.id) && Objects.equal(query, retrieval.query)
                && Objects.equal(metadata, retrieval.metadata);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id, distance, query, metadata);
    }
}
