package com.tencent.supersonic;

import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.chat.api.pojo.request.*;
import com.tencent.supersonic.chat.api.pojo.response.ParseResp;
import com.tencent.supersonic.chat.parser.ParseMode;
import com.tencent.supersonic.chat.plugin.Plugin;
import com.tencent.supersonic.chat.query.plugin.ParamOption;
import com.tencent.supersonic.chat.query.plugin.WebBase;
import com.tencent.supersonic.chat.service.ChatService;
import com.tencent.supersonic.chat.service.ConfigService;
import com.tencent.supersonic.chat.service.PluginService;
import com.tencent.supersonic.chat.service.QueryService;
import com.tencent.supersonic.common.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@Slf4j
public class ConfigureDemo implements ApplicationListener<ApplicationReadyEvent> {
    @Autowired
    private QueryService queryService;
    @Autowired
    private ChatService chatService;
    @Autowired
    protected ConfigService configService;
    @Autowired
    private PluginService pluginService;

    private User user = User.getFakeUser();

    private void parseAndExecute(int chatId, String queryText) throws Exception {
        QueryReq queryRequest = new QueryReq();
        queryRequest.setQueryText(queryText);
        queryRequest.setChatId(chatId);
        queryRequest.setUser(User.getFakeUser());
        ParseResp parseResp = queryService.performParsing(queryRequest);

        ExecuteQueryReq executeReq = new ExecuteQueryReq();
        executeReq.setQueryText(queryRequest.getQueryText());
        executeReq.setParseInfo(parseResp.getSelectedParses().get(0));
        executeReq.setChatId(parseResp.getChatId());
        executeReq.setUser(queryRequest.getUser());
        queryService.performExecution(executeReq);
    }

    public void addSampleChats() throws Exception {
        chatService.addChat(user, "样例对话1");

        parseAndExecute(1, "超音数 访问次数");
        parseAndExecute(1, "按部门统计");
        parseAndExecute(1, "查询近30天");
    }

    public void addSampleChats2() throws Exception {
        chatService.addChat(user, "样例对话2");

        parseAndExecute(2, "alice 停留时长");
        parseAndExecute(2, "对比alice和lucy的访问次数");
        parseAndExecute(2, "访问次数最高的部门");
    }

    public void addDemoChatConfig_1() {
        ChatConfigBaseReq chatConfigBaseReq = new ChatConfigBaseReq();
        chatConfigBaseReq.setDomainId(1L);

        ChatDetailConfigReq chatDetailConfig = new ChatDetailConfigReq();
        ChatDefaultConfigReq chatDefaultConfigDetail = new ChatDefaultConfigReq();
        List<Long> dimensionIds_0 = Arrays.asList(1L, 2L);
        List<Long> metricIds_0 = Arrays.asList(1L);
        chatDefaultConfigDetail.setDimensionIds(dimensionIds_0);
        chatDefaultConfigDetail.setMetricIds(metricIds_0);
        chatDefaultConfigDetail.setUnit(7);
        chatDefaultConfigDetail.setPeriod("DAY");
        chatDetailConfig.setChatDefaultConfig(chatDefaultConfigDetail);
        ItemVisibility visibility_0 = new ItemVisibility();
        chatDetailConfig.setVisibility(visibility_0);
        chatConfigBaseReq.setChatDetailConfig(chatDetailConfig);


        ChatAggConfigReq chatAggConfig = new ChatAggConfigReq();
        ChatDefaultConfigReq chatDefaultConfigAgg = new ChatDefaultConfigReq();
        List<Long> dimensionIds_1 = Arrays.asList(1L, 2L);
        List<Long> metricIds_1 = Arrays.asList(1L);
        chatDefaultConfigAgg.setDimensionIds(dimensionIds_1);
        chatDefaultConfigAgg.setMetricIds(metricIds_1);
        chatDefaultConfigAgg.setUnit(7);
        chatDefaultConfigAgg.setPeriod("DAY");
        chatDefaultConfigAgg.setTimeMode(ChatDefaultConfigReq.TimeMode.RECENT);
        chatAggConfig.setChatDefaultConfig(chatDefaultConfigAgg);
        ItemVisibility visibility_1 = new ItemVisibility();
        chatAggConfig.setVisibility(visibility_1);
        chatConfigBaseReq.setChatAggConfig(chatAggConfig);

        List<RecommendedQuestionReq> recommendedQuestions = new ArrayList<>();
        RecommendedQuestionReq recommendedQuestionReq_0 = new RecommendedQuestionReq("超音数访问次数");
        RecommendedQuestionReq recommendedQuestionReq_1 = new RecommendedQuestionReq("超音数访问人数");
        RecommendedQuestionReq recommendedQuestionReq_2 = new RecommendedQuestionReq("超音数按部门访问次数");
        recommendedQuestions.add(recommendedQuestionReq_0);
        recommendedQuestions.add(recommendedQuestionReq_1);
        recommendedQuestions.add(recommendedQuestionReq_2);
        chatConfigBaseReq.setRecommendedQuestions(recommendedQuestions);

        configService.addConfig(chatConfigBaseReq, user);
    }

    public void addDemoChatConfig_2() {
        ChatConfigBaseReq chatConfigBaseReq = new ChatConfigBaseReq();
        chatConfigBaseReq.setDomainId(2L);

        ChatDetailConfigReq chatDetailConfig = new ChatDetailConfigReq();
        ChatDefaultConfigReq chatDefaultConfigDetail = new ChatDefaultConfigReq();
        List<Long> dimensionIds_0 = Arrays.asList(4L, 5L, 6L, 7L);
        List<Long> metricIds_0 = Arrays.asList(4L);
        chatDefaultConfigDetail.setDimensionIds(dimensionIds_0);
        chatDefaultConfigDetail.setMetricIds(metricIds_0);
        chatDefaultConfigDetail.setUnit(7);
        chatDefaultConfigDetail.setPeriod("DAY");
        chatDetailConfig.setChatDefaultConfig(chatDefaultConfigDetail);
        ItemVisibility visibility_0 = new ItemVisibility();
        chatDetailConfig.setVisibility(visibility_0);
        chatConfigBaseReq.setChatDetailConfig(chatDetailConfig);


        ChatAggConfigReq chatAggConfig = new ChatAggConfigReq();
        ChatDefaultConfigReq chatDefaultConfigAgg = new ChatDefaultConfigReq();
        List<Long> dimensionIds_1 = Arrays.asList(4L, 5L, 6L, 7L);
        List<Long> metricIds_1 = Arrays.asList(4L);
        chatDefaultConfigAgg.setDimensionIds(dimensionIds_1);
        chatDefaultConfigAgg.setMetricIds(metricIds_1);
        chatDefaultConfigAgg.setUnit(7);
        chatDefaultConfigAgg.setPeriod("DAY");
        chatDefaultConfigAgg.setTimeMode(ChatDefaultConfigReq.TimeMode.RECENT);
        chatAggConfig.setChatDefaultConfig(chatDefaultConfigAgg);
        ItemVisibility visibility_1 = new ItemVisibility();
        chatAggConfig.setVisibility(visibility_1);
        chatConfigBaseReq.setChatAggConfig(chatAggConfig);

        List<RecommendedQuestionReq> recommendedQuestions = new ArrayList<>();
        chatConfigBaseReq.setRecommendedQuestions(recommendedQuestions);

        configService.addConfig(chatConfigBaseReq, user);
    }

    private void addPlugin_1() {
        Plugin plugin_1 = new Plugin();
        plugin_1.setType("WEB_PAGE");
        plugin_1.setDomainList(Arrays.asList(1L));
        plugin_1.setPattern("访问情况");
        plugin_1.setParseModeConfig(null);
        plugin_1.setName("访问情况");
        WebBase webBase = new WebBase();
        webBase.setUrl("www.test.com");
        ParamOption paramOption = new ParamOption();
        paramOption.setKey("name");
        paramOption.setParamType(ParamOption.ParamType.SEMANTIC);
        paramOption.setElementId(2L);
        paramOption.setDomainId(1L);
        List<ParamOption> paramOptions = Arrays.asList(paramOption);
        webBase.setParamOptions(paramOptions);
        plugin_1.setConfig(JsonUtil.toString(webBase));

        pluginService.createPlugin(plugin_1, user);
    }

    private void addPlugin_2() {
        Plugin plugin_1 = new Plugin();
        plugin_1.setType("DSL");
        plugin_1.setDomainList(new ArrayList<>());
        plugin_1.setPattern("");
        plugin_1.setParseMode(ParseMode.FUNCTION_CALL);
        plugin_1.setParseModeConfig(null);
        plugin_1.setName("访问情况");
        plugin_1.setConfig("");
        pluginService.createPlugin(plugin_1, user);
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent applicationReadyEvent) {
        try {
            addDemoChatConfig_1();
            addDemoChatConfig_2();
            addPlugin_1();
            addPlugin_2();
            addSampleChats();
            addSampleChats2();
        } catch (Exception e) {
            log.error("Failed to add sample chats", e);
        }
    }


}
