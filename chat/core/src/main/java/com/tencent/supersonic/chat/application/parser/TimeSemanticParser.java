package com.tencent.supersonic.chat.application.parser;

import com.tencent.supersonic.chat.api.component.SemanticParser;
import com.tencent.supersonic.chat.api.component.SemanticQuery;
import com.tencent.supersonic.chat.api.pojo.ChatContext;
import com.tencent.supersonic.chat.api.request.QueryContextReq;
import com.tencent.supersonic.chat.application.query.MetricSemanticQuery;
import com.tencent.supersonic.chat.application.query.RuleSemanticQuery;
import com.tencent.supersonic.chat.application.query.RuleSemanticQueryManager;
import com.tencent.supersonic.common.constant.Constants;
import com.tencent.supersonic.common.pojo.DateConf;
import com.tencent.supersonic.common.pojo.SchemaItem;
import com.tencent.supersonic.semantic.api.core.enums.TimeDimensionEnum;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.collections.CollectionUtils;
import org.apache.logging.log4j.util.Strings;

public class TimeSemanticParser implements SemanticParser {

    private static final Pattern recentPeriodPattern = Pattern.compile(
            ".*(?<periodStr>(近|过去)((?<enNum>\\d+)|(?<zhNum>[一二三四五六七八九十百千万亿]+))个?(?<zhPeriod>[天周月年])).*");

    private int zhNumParse(String zhNumStr) {
        Stack<Integer> stack = new Stack<>();
        String numStr = "一二三四五六七八九";
        String unitStr = "十百千万亿";

        String[] ssArr = zhNumStr.split("");
        for (String e : ssArr) {
            int numIndex = numStr.indexOf(e);
            int unitIndex = unitStr.indexOf(e);
            if (numIndex != -1) {
                stack.push(numIndex + 1);
            } else if (unitIndex != -1) {
                int unitNum = (int) Math.pow(10, unitIndex + 1);
                if (stack.isEmpty()) {
                    stack.push(unitNum);
                } else {
                    stack.push(stack.pop() * unitNum);
                }
            }
        }

        return stack.stream().mapToInt(s -> s).sum();
    }

    @Override
    public void parse(QueryContextReq queryContext, ChatContext chatContext) {
        Matcher m = recentPeriodPattern.matcher(queryContext.getQueryText());
        if (m.matches()) {
            int num = 0;
            String enNum = m.group("enNum");
            String zhNum = m.group("zhNum");
            if (enNum != null) {
                num = Integer.parseInt(enNum);
            } else if (zhNum != null) {
                num = zhNumParse(zhNum);
            }
            if (num > 0) {
                DateConf info = new DateConf();
                String zhPeriod = m.group("zhPeriod");
                int days;
                info.setPeriod(Constants.DAY);
                switch (zhPeriod) {
                    case "周":
                        days = 7;
                        //info.setPeriod(Constants.WEEK);
                        break;
                    case "月":
                        days = 30;
                        //info.setPeriod(Constants.MONTH);
                        break;
                    case "年":
                        days = 365;
                        //info.setPeriod(Constants.YEAR);
                        break;
                    default:
                        days = 1;
                        info.setPeriod(Constants.DAY);
                }
                days = days * num;
                info.setDateMode(DateConf.DateMode.RECENT_UNITS);
                String text = "近" + num + zhPeriod;
                if (Strings.isNotEmpty(m.group("periodStr"))) {
                    text = m.group("periodStr");
                }
                info.setText(text);
                info.setStartDate(LocalDate.now().minusDays(days).toString());
                info.setUnit(days);
                //queryContext.getParseInfo().setDateInfo(info);
                for (SemanticQuery query : queryContext.getCandidateQueries()) {
                    if (query instanceof MetricSemanticQuery) {
                        query.getParseInfo().setDateInfo(info);
                    }
                }
                doParseOnlyTime(queryContext, chatContext, info);
            }
        }
    }

    protected void doParseOnlyTime(QueryContextReq queryContext, ChatContext chatContext, DateConf info) {
        if (!queryContext.getCandidateQueries().isEmpty() || chatContext.getParseInfo() == null || Objects.isNull(
                info.getText())) {
            return;
        }
        if (info.getText().equals(queryContext.getQueryText()) && queryContext.getMapInfo().getDomainElementMatches()
                .isEmpty()
        ) {
            if (Objects.nonNull(chatContext.getParseInfo().getQueryMode()) && Objects.nonNull(
                    chatContext.getParseInfo().getDomainId()) && chatContext.getParseInfo().getDomainId() > 0) {
                if (Objects.nonNull(chatContext.getParseInfo().getDateInfo()) && !chatContext.getParseInfo()
                        .getDateInfo().getPeriod().equals(info.getPeriod())) {
                    if (!CollectionUtils.isEmpty(chatContext.getParseInfo().getDimensions())) {
                        String dateField = TimeDimensionEnum.DAY.getName();
                        if (Constants.MONTH.equals(chatContext.getParseInfo().getDateInfo().getPeriod())) {
                            dateField = TimeDimensionEnum.MONTH.getName();
                        }
                        if (Constants.WEEK.equals(chatContext.getParseInfo().getDateInfo().getPeriod())) {
                            dateField = TimeDimensionEnum.WEEK.getName();
                        }
                        Set<SchemaItem> dimensions = new HashSet<>();
                        for (SchemaItem schemaItem : chatContext.getParseInfo().getDimensions()) {
                            if (schemaItem.getBizName().equals(dateField)) {
                                continue;
                            }
                            dimensions.add(schemaItem);
                        }
                        chatContext.getParseInfo().setDimensions(dimensions);
                    }
                }
                chatContext.getParseInfo().setDateInfo(info);
                RuleSemanticQuery semanticQuery = RuleSemanticQueryManager.create(
                        chatContext.getParseInfo().getQueryMode());
                semanticQuery.setParseInfo(chatContext.getParseInfo());
                queryContext.getCandidateQueries().add(semanticQuery);
            }
        }
    }
}