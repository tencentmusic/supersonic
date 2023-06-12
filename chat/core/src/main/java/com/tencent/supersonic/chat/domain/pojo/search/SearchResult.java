package com.tencent.supersonic.chat.domain.pojo.search;

import com.tencent.supersonic.chat.api.pojo.SchemaElementType;
import java.util.Objects;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Setter
@Getter
public class SearchResult {

    private String recommend;

    private String subRecommend;

    private String domainName;

    private Integer domainId;

    private SchemaElementType schemaElementType;

    private boolean isComplete = true;

    public SearchResult(String recommend, String subRecommend, String className, Integer domainId,
            SchemaElementType schemaElementType) {
        this.recommend = recommend;
        this.subRecommend = subRecommend;
        this.domainName = className;
        this.domainId = domainId;
        this.schemaElementType = schemaElementType;
    }

    public SearchResult(String recommend, String subRecommend, String className, Integer domainId, boolean isComplete) {
        this.recommend = recommend;
        this.subRecommend = subRecommend;
        this.domainName = className;
        this.domainId = domainId;
        this.isComplete = isComplete;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SearchResult searchResult1 = (SearchResult) o;
        return Objects.equals(recommend, searchResult1.recommend) && Objects.equals(domainName,
                searchResult1.domainName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(recommend, domainName);
    }
}
