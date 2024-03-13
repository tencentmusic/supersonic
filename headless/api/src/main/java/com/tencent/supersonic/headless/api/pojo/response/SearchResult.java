package com.tencent.supersonic.headless.api.pojo.response;

import com.tencent.supersonic.headless.api.pojo.SchemaElementType;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

@Data
@Setter
@Getter
@Builder
public class SearchResult {

    private String recommend;

    private String subRecommend;

    private String modelName;

    private Long modelId;

    private SchemaElementType schemaElementType;

    private boolean isComplete = true;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SearchResult searchResult1 = (SearchResult) o;
        return Objects.equals(recommend, searchResult1.recommend) && Objects.equals(modelName,
                searchResult1.modelName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(recommend, modelName);
    }
}
