package com.tencent.supersonic.chat.server.plugin.build.react;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.tencent.supersonic.auth.api.authentication.config.AuthenticationConfig;
import com.tencent.supersonic.chat.api.pojo.enums.MemoryStatus;
import com.tencent.supersonic.chat.api.pojo.response.ChatParseResp;
import com.tencent.supersonic.chat.api.pojo.response.QueryResult;
import com.tencent.supersonic.chat.server.plugin.ChatPlugin;
import com.tencent.supersonic.chat.server.plugin.PluginParseResult;
import com.tencent.supersonic.chat.server.plugin.PluginQueryManager;
import com.tencent.supersonic.chat.server.plugin.PluginRecallResult;
import com.tencent.supersonic.chat.server.plugin.build.PluginSemanticQuery;
import com.tencent.supersonic.chat.server.plugin.build.webpage.WebPageResp;
import com.tencent.supersonic.chat.server.pojo.ChatMemory;
import com.tencent.supersonic.chat.server.pojo.ExecuteContext;
import com.tencent.supersonic.chat.server.pojo.ParseContext;
import com.tencent.supersonic.chat.server.service.MemoryService;
import com.tencent.supersonic.common.pojo.Constants;
import com.tencent.supersonic.common.pojo.QueryColumn;
import com.tencent.supersonic.common.pojo.Text2SQLExemplar;
import com.tencent.supersonic.common.service.impl.ExemplarServiceImpl;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.common.util.JsonUtil;
import com.tencent.supersonic.headless.api.pojo.response.QueryState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


@Slf4j
@Component
public class ReActQuery extends PluginSemanticQuery {

    public static String QUERY_MODE = "REACT";
    @Autowired
    private MemoryService memoryService;

    public ReActQuery() {
        PluginQueryManager.register(QUERY_MODE, this);
    }

    public void setChatMemory(ExecuteContext executeContext, QueryResult res) {
        Text2SQLExemplar exemplar = JsonUtil.toObject(
                JsonUtil.toString(parseInfo.getProperties().get(Text2SQLExemplar.PROPERTY_KEY2)),
                Text2SQLExemplar.class);

        memoryService
                .createMemory(ChatMemory.builder().queryId(executeContext.getRequest().getQueryId())
                        .agentId(executeContext.getAgent().getId()).status(MemoryStatus.PENDING)
                        .question(exemplar.getQuestion()).sideInfo(exemplar.getSideInfo())
                        .dbSchema(exemplar.getDbSchema()).s2sql(exemplar.getSql())
                        .createdBy(executeContext.getRequest().getUser().getName())
                        .updatedBy(executeContext.getRequest().getUser().getName())
                        .createdAt(new Date()).build());
    }

    private JSONObject dealParam(String queryText, String paramStr, JSONArray paramOptions) {
        JSONObject param = JSON.parseObject(paramStr);// 真实入参
        param.put("queryText", queryText);
        return param;
    }

    private void setResult(Map<String, Object> res, QueryResult queryResult) {
        if (res.get("resultList") != null) {
            queryResult.setQueryResults((List<Map<String, Object>>) res.get("resultList"));
        }
        if (res.get("textSummary") != null) {
            queryResult.setTextSummary((String) res.get("textSummary"));
        }
        if (res.get("columns") != null) {
            queryResult.setQueryColumns((List<QueryColumn>) res.get("columns"));
        }
        queryResult.setTextResult((String) res.get("textResult"));
        if (res.get("textResult") == null) {
            // TODO 无法解析的情况
            queryResult.setQueryState(QueryState.INVALID);
        } else {
            queryResult.setQueryState(QueryState.SUCCESS);
        }
    }

    private void localServerDeal(String queryText, JSONObject parseModeConfig,
            Text2SQLExemplar exemplar, JSONObject config, QueryResult queryResult) {
        String name = parseModeConfig.getString("name");
        String tmp[] = name.split(":");
        String reactServer = tmp.length == 2 ? tmp[1] : tmp[0];
        ReactServer server = (ReactServer) ContextUtils.getBean(reactServer);
        JSONObject param =
                dealParam(queryText, exemplar.getDbSchema(), config.getJSONArray("paramOptions"));
        param.put("url", config.getString("url"));
        Map<String, Object> res = server.invoke(param);
        setResult(res, queryResult);
    }

    private void httpServer(String queryText, JSONObject parseModeConfig, Text2SQLExemplar exemplar,
            JSONObject config, QueryResult queryResult) {
        JSONObject param =
                dealParam(queryText, exemplar.getDbSchema(), config.getJSONArray("paramOptions"));
        URI requestUrl =
                UriComponentsBuilder.fromHttpUrl(config.getString("url")).build().encode().toUri();
        RestTemplate restTemplate = ContextUtils.getBean(RestTemplate.class);
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(param.toString(), headers);
            ResponseEntity responseEntity =
                    restTemplate.exchange(requestUrl, HttpMethod.POST, entity, Object.class);
            Map<String, Object> response = JsonUtil.objectToMap(responseEntity.getBody());
            setResult(response, queryResult);
        } catch (Exception e) {
            log.info("Exception:{}", e.getMessage());
        }
    }

    @Override
    public QueryResult build() {
        QueryResult queryResult = new QueryResult();
        queryResult.setQueryMode(QUERY_MODE);
        Map<String, Object> properties = parseInfo.getProperties();
        PluginParseResult pluginParseResult = JsonUtil.toObject(
                JsonUtil.toString(properties.get(Constants.CONTEXT)), PluginParseResult.class);
        ChatPlugin plugin = pluginParseResult.getPlugin();
        JSONObject config = JSON.parseObject(plugin.getConfig());
        JSONObject parseModeConfig = JSON.parseObject(plugin.getParseModeConfig());
        Text2SQLExemplar exemplar = JsonUtil.toObject(
                JsonUtil.toString(parseInfo.getProperties().get(Text2SQLExemplar.PROPERTY_KEY2)),
                Text2SQLExemplar.class);
        String url = config.getString("url");
        String name = parseModeConfig.getString("name");
        if (url.startsWith("http")) {
            httpServer(pluginParseResult.getQueryText(), parseModeConfig, exemplar, config,
                    queryResult);
        } else if (name.startsWith("localServer:") || url.equals("localServer")) {
            localServerDeal(pluginParseResult.getQueryText(), parseModeConfig, exemplar, config,
                    queryResult);
        } else {
            queryResult.setQueryState(QueryState.INVALID);
            // TODO 无法解析的情况
        }
        return queryResult;
    }
}
