package com.tencent.supersonic.chat.parser.llm.time;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.tencent.supersonic.chat.api.component.SemanticParser;
import com.tencent.supersonic.chat.api.component.SemanticQuery;
import com.tencent.supersonic.chat.api.pojo.ChatContext;
import com.tencent.supersonic.chat.api.pojo.QueryContext;
import com.tencent.supersonic.common.pojo.DateConf;
import com.tencent.supersonic.common.util.ChatGptHelper;
import com.tencent.supersonic.common.util.ContextUtils;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LLMTimeEnhancementParse implements SemanticParser {


    @Override
    public void parse(QueryContext queryContext, ChatContext chatContext) {
        log.info("before queryContext:{},chatContext:{}", queryContext, chatContext);
        ChatGptHelper chatGptHelper = ContextUtils.getBean(ChatGptHelper.class);
        try {
            String inferredTime = chatGptHelper.inferredTime(queryContext.getRequest().getQueryText());
            if (!queryContext.getCandidateQueries().isEmpty()) {
                for (SemanticQuery query : queryContext.getCandidateQueries()) {
                    DateConf dateInfo = query.getParseInfo().getDateInfo();
                    JSONObject jsonObject = JSON.parseObject(inferredTime);
                    if (jsonObject.containsKey("date")) {
                        dateInfo.setDateMode(DateConf.DateMode.BETWEEN);
                        dateInfo.setStartDate(jsonObject.getString("date"));
                        dateInfo.setEndDate(jsonObject.getString("date"));
                        query.getParseInfo().setDateInfo(dateInfo);
                    } else if (jsonObject.containsKey("start")) {
                        dateInfo.setDateMode(DateConf.DateMode.BETWEEN);
                        dateInfo.setStartDate(jsonObject.getString("start"));
                        dateInfo.setEndDate(jsonObject.getString("end"));
                        query.getParseInfo().setDateInfo(dateInfo);
                    }
                }
            }
        } catch (Exception exception) {
            log.error("{} parse error,this reason is:{}", LLMTimeEnhancementParse.class.getSimpleName(),
                    (Object) exception.getStackTrace());
        }

        log.info("{} after queryContext:{},chatContext:{}",
                LLMTimeEnhancementParse.class.getSimpleName(), queryContext, chatContext);
    }


}
