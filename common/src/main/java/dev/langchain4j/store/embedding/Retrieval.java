package dev.langchain4j.store.embedding;

import com.google.common.base.Objects;
import com.tencent.supersonic.common.pojo.Constants;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;

@Data
public class Retrieval {

    protected String id;

    protected double similarity;

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
        return Double.compare(retrieval.similarity, similarity) == 0
                && Objects.equal(id, retrieval.id) && Objects.equal(query, retrieval.query)
                && Objects.equal(metadata, retrieval.metadata);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id, similarity, query, metadata);
    }
}
