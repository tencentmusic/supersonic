package com.tencent.supersonic.chat.api.pojo.response;

import java.util.List;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
public class SearchResp {

    private List<SearchResult> searchResults;

    public SearchResp(List<SearchResult> searchResults) {
        this.searchResults = searchResults;
    }
}
