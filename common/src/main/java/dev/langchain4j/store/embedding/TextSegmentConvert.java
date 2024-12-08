package dev.langchain4j.store.embedding;

import com.alibaba.fastjson.JSONObject;
import com.tencent.supersonic.common.pojo.Constants;
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
        return dataItems.stream().map(item -> {
            // suffix with underscore to avoid embedding issue
            DataItem newItem = DataItem.builder().domainId(item.getDomainId())
                    .bizName(item.getBizName()).type(item.getType()).newName(item.getNewName())
                    .defaultAgg(item.getDefaultAgg()).name(item.getName())
                    .id(item.getId() + Constants.UNDERLINE)
                    .modelId(item.getModelId() + Constants.UNDERLINE)
                    .domainId(item.getDomainId() + Constants.UNDERLINE).build();

            Map meta = JSONObject.parseObject(JSONObject.toJSONString(newItem), Map.class);
            TextSegment textSegment = TextSegment.from(newItem.getName(), new Metadata(meta));
            addQueryId(textSegment, newItem.getId() + newItem.getType().name().toLowerCase());
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
