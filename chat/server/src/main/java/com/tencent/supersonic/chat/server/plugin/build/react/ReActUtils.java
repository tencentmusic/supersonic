package com.tencent.supersonic.chat.server.plugin.build.react;

import com.tencent.supersonic.chat.api.pojo.enums.MemoryStatus;
import com.tencent.supersonic.chat.server.plugin.PluginRecallResult;
import com.tencent.supersonic.chat.server.pojo.ChatMemory;
import com.tencent.supersonic.chat.server.pojo.ExecuteContext;
import com.tencent.supersonic.chat.server.pojo.ParseContext;
import com.tencent.supersonic.chat.server.service.MemoryService;
import com.tencent.supersonic.common.pojo.Text2SQLExemplar;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.common.util.JsonUtil;

import java.util.Date;
import java.util.Map;

public class ReActUtils {
    public static boolean fromOtherAgent(Map<String, Object> properties) {
        return properties.containsKey(Text2SQLExemplar.PROPERTY_KEY2);
    }

    public static void saveMemory(ParseContext parseContext,
            PluginRecallResult pluginRecallResult) {
        Text2SQLExemplar exemplar =
                Text2SQLExemplar.builder().question(parseContext.getRequest().getQueryText())
                        .sideInfo(pluginRecallResult.getLlmResp().getSideInfo())
                        .dbSchema(pluginRecallResult.getLlmResp().getSchema())
                        .sql(pluginRecallResult.getLlmResp().getSqlOutput()).build();
        MemoryService memoryService = ContextUtils.getBean(MemoryService.class);
        saveMemory(exemplar, parseContext.getRequest().getQueryId(),
                parseContext.getAgent().getId(), parseContext.getRequest().getUser().getName(),
                memoryService);

    }

    private static void saveMemory(Text2SQLExemplar exemplar, Long queryId, Integer agentId,
            String userName, MemoryService memoryService) {
        memoryService.createMemory(
                ChatMemory.builder().queryId(queryId).agentId(agentId).status(MemoryStatus.PENDING)
                        .question(exemplar.getQuestion()).sideInfo(exemplar.getSideInfo())
                        .dbSchema(exemplar.getDbSchema()).s2sql(exemplar.getSql())
                        .createdBy(userName).updatedBy(userName).createdAt(new Date()).build());
    }

    public static Integer deal(Map<String, Object> properties, MemoryService memoryService,
            ExecuteContext executeContext) {
        // 个性修改，react 转移过来的请求
        Text2SQLExemplar exemplar =
                JsonUtil.toObject(JsonUtil.toString(properties.get(Text2SQLExemplar.PROPERTY_KEY2)),
                        Text2SQLExemplar.class);
        saveMemory(exemplar, executeContext.getRequest().getQueryId(),
                executeContext.getAgent().getId(), executeContext.getRequest().getUser().getName(),
                memoryService);
        return Integer.valueOf(exemplar.getSideInfo());

    }
}
