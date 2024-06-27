package dev.langchain4j.store.embedding;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class RetrieveQuery {

    private List<String> queryTextsList;

    private Map<String, String> filterCondition;

    private List<List<Double>> queryEmbeddings;


}
