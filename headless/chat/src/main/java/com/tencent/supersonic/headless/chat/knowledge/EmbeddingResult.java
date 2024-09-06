package com.tencent.supersonic.headless.chat.knowledge;

import com.google.common.base.Objects;
import lombok.Data;
import lombok.ToString;

import java.util.Map;

@Data
@ToString
public class EmbeddingResult extends MapResult {

    private String id;

    private double distance;

    private Map<String, String> metadata;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        EmbeddingResult that = (EmbeddingResult) o;
        return Objects.equal(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
