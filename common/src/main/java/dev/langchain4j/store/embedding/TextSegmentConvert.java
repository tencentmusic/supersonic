package dev.langchain4j.store.embedding;


import com.alibaba.fastjson.JSONObject;
import com.tencent.supersonic.common.pojo.DataItem;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;
import lombok.Data;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Data
public class TextSegmentConvert {

    public static final String QUERY_ID = "queryId";

    public static List<TextSegment> convertToEmbedding(List<DataItem> dataItems) {
        return dataItems.stream().map(dataItem -> {
            Map meta = JSONObject.parseObject(JSONObject.toJSONString(dataItem), Map.class);
            TextSegment textSegment = TextSegment.from(dataItem.getName(), new Metadata(meta));
            addQueryId(textSegment, dataItem.getId() + dataItem.getType().name().toLowerCase());
            return textSegment;
        }).collect(Collectors.toList());
    }

    public static void addQueryId(TextSegment textSegment, String queryId) {
        textSegment.metadata().put(QUERY_ID, queryId);
    }

    public static String getQueryId(TextSegment textSegment) {
        if (Objects.isNull(textSegment) || Objects.isNull(textSegment.metadata())) {
            return null;
        }
        return textSegment.metadata().get(QUERY_ID);
    }
}
