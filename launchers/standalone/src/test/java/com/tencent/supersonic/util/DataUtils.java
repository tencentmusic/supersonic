package com.tencent.supersonic.util;

import com.tencent.supersonic.chat.api.pojo.request.ChatParseReq;
import com.tencent.supersonic.common.pojo.DateConf;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.common.pojo.enums.DatePeriodEnum;
import com.tencent.supersonic.common.pojo.enums.FilterOperatorEnum;
import com.tencent.supersonic.headless.api.pojo.SchemaElement;
import com.tencent.supersonic.headless.api.pojo.request.QueryFilter;

import java.util.HashSet;
import java.util.Set;

import static java.time.LocalDate.now;

public class DataUtils {

    public static final Integer ONE_TURNS_CHAT_ID = 10;
    private static final User user_test = User.getDefaultUser();

    public static User getUser() {
        return user_test;
    }

    public static User getUserAlice() {
        return User.get(5L, "alice");
    }

    public static User getUserJack() {
        return User.get(2L, "jack");
    }

    public static User getUserTom() {
        return User.get(3L, "tom");
    }

    public static ChatParseReq getChatParseReq(Integer id, Integer agentId, String query,
            boolean enableLLM) {
        ChatParseReq chatParseReq = new ChatParseReq();
        chatParseReq.setQueryText(query);
        chatParseReq.setChatId(id);
        chatParseReq.setAgentId(agentId);
        chatParseReq.setUser(user_test);
        chatParseReq.setDisableLLM(!enableLLM);
        return chatParseReq;
    }

    public static SchemaElement getSchemaElement(String name) {
        return SchemaElement.builder().name(name).build();
    }

    public static QueryFilter getFilter(String bizName, FilterOperatorEnum filterOperatorEnum,
            Object value, String name, Long elementId) {
        QueryFilter filter = new QueryFilter();
        filter.setBizName(bizName);
        filter.setOperator(filterOperatorEnum);
        filter.setValue(value);
        filter.setName(name);
        filter.setElementID(elementId);
        return filter;
    }

    public static DateConf getDateConf(Integer unit, DateConf.DateMode dateMode,
            DatePeriodEnum period) {
        DateConf dateInfo = new DateConf();
        dateInfo.setUnit(unit);
        dateInfo.setDateMode(dateMode);
        dateInfo.setPeriod(period);
        dateInfo.setStartDate(now().minusDays(unit).toString());
        dateInfo.setEndDate(now().toString());
        return dateInfo;
    }

    public static DateConf getDateConf(DateConf.DateMode dateMode, Integer unit,
            DatePeriodEnum period, String startDate, String endDate) {
        DateConf dateInfo = new DateConf();
        dateInfo.setDateField("imp_date");
        dateInfo.setUnit(unit);
        dateInfo.setDateMode(dateMode);
        dateInfo.setPeriod(period);
        dateInfo.setStartDate(startDate);
        dateInfo.setEndDate(endDate);
        dateInfo.setDateField("imp_date");
        return dateInfo;
    }

    public static Set<Long> getMetricAgentIModelIds() {
        Set<Long> result = new HashSet<>();
        result.add(1L);
        result.add(2L);
        result.add(3L);
        return result;
    }

}
