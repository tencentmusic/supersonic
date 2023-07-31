package com.tencent.supersonic.chat.api.pojo.response;

import com.tencent.supersonic.chat.api.pojo.SchemaElementType;
import java.util.Objects;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Setter
@Getter
@Builder
public class SearchResult {

    private String recommend;

    private String subRecommend;

    private String domainName;

    private Long domainId;

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
        return Objects.equals(recommend, searchResult1.recommend) && Objects.equals(domainName,
                searchResult1.domainName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(recommend, domainName);
    }
}
