package com.tencent.supersonic.headless.chat.parser.rule;

import com.tencent.supersonic.common.pojo.DateConf;
import com.tencent.supersonic.common.pojo.enums.DatePeriodEnum;
import com.tencent.supersonic.headless.api.pojo.DataSetSchema;
import com.tencent.supersonic.headless.api.pojo.SchemaElement;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.headless.chat.ChatQueryContext;
import com.tencent.supersonic.headless.chat.parser.SemanticParser;
import com.tencent.supersonic.headless.chat.query.SemanticQuery;
import com.xkzhangsan.time.nlp.TimeNLP;
import com.xkzhangsan.time.nlp.TimeNLPUtil;
import lombok.extern.slf4j.Slf4j;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * TimeRangeParser extracts time range specified in the user query based on keyword matching.
 * Currently, it supports two kinds of expression: 1. Recent unit: 近N天/周/月/年、过去N天/周/月/年 2. Concrete
 * date: 2023年11月15日、20231115
 */
@Slf4j
public class TimeRangeParser implements SemanticParser {

    private static final Pattern RECENT_PATTERN_CN = Pattern.compile(
            ".*(?<periodStr>(近|过去)((?<enNum>\\d+)|(?<zhNum>[一二三四五六七八九十百千万亿]+))个?(?<zhPeriod>[天周月年])).*");
    private static final Pattern DATE_PATTERN_NUMBER = Pattern.compile("(\\d{8})");
    private static final DateFormat DATE_FORMAT_NUMBER = new SimpleDateFormat("yyyyMMdd");
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    @Override
    public void parse(ChatQueryContext queryContext) {
        if (queryContext.getCandidateQueries().isEmpty()) {
            return;
        }

        String queryText = queryContext.getRequest().getQueryText();
        DateConf dateConf = parseRecent(queryText);
        if (dateConf == null) {
            dateConf = parseDateNumber(queryText);
        }
        if (dateConf == null) {
            dateConf = parseDateCN(queryText);
        }
        if (dateConf != null) {
            updateQueryContext(queryContext, dateConf);
        }
    }

    private void updateQueryContext(ChatQueryContext queryContext, DateConf dateConf) {
        for (SemanticQuery query : queryContext.getCandidateQueries()) {
            SemanticParseInfo parseInfo = query.getParseInfo();
            if (queryContext.containsPartitionDimensions(parseInfo.getDataSetId())) {
                DataSetSchema dataSetSchema = queryContext.getSemanticSchema().getDataSetSchemaMap()
                        .get(parseInfo.getDataSetId());
                SchemaElement partitionDimension = dataSetSchema.getPartitionDimension();
                dateConf.setDateField(partitionDimension.getName());
                parseInfo.setDateInfo(dateConf);
            }
            parseInfo.setScore(parseInfo.getScore() + dateConf.getDetectWord().length());
        }
    }

    private DateConf parseDateCN(String queryText) {
        try {
            List<TimeNLP> times = TimeNLPUtil.parse(queryText);
            if (times.isEmpty()) {
                return null;
            }

            Date startDate = times.get(0).getTime();
            String detectWord = times.get(0).getTimeExpression();
            Date endDate = times.size() > 1 ? times.get(1).getTime() : startDate;

            if (times.size() > 1) {
                detectWord += "~" + times.get(1).getTimeExpression();
            }

            return getDateConf(startDate, endDate, detectWord);
        } catch (Exception e) {
            return null;
        }
    }

    private DateConf parseDateNumber(String queryText) {
        Matcher dateMatcher = DATE_PATTERN_NUMBER.matcher(queryText);
        if (!dateMatcher.find()) {
            return null;
        }

        String startDateStr = dateMatcher.group();
        String detectWord = startDateStr;
        String endDateStr = dateMatcher.find() ? dateMatcher.group() : startDateStr;

        if (!startDateStr.equals(endDateStr)) {
            detectWord += "~" + endDateStr;
        }

        try {
            Date startDate = DATE_FORMAT_NUMBER.parse(startDateStr);
            Date endDate = DATE_FORMAT_NUMBER.parse(endDateStr);
            return getDateConf(startDate, endDate, detectWord);
        } catch (ParseException e) {
            return null;
        }
    }

    private DateConf parseRecent(String queryText) {
        Matcher matcher = RECENT_PATTERN_CN.matcher(queryText);
        if (!matcher.matches()) {
            return null;
        }
        int num = parseNumber(matcher);
        if (num <= 0) {
            return null;
        }
        String zhPeriod = matcher.group("zhPeriod");
        int days = getDaysByPeriod(zhPeriod) * num;
        String detectWord = matcher.group("periodStr");

        DateConf info = new DateConf();
        info.setPeriod(DatePeriodEnum.fromChName(zhPeriod));
        info.setDateMode(DateConf.DateMode.BETWEEN);
        info.setDetectWord(detectWord);
        info.setStartDate(LocalDate.now().minusDays(days).toString());
        info.setEndDate(LocalDate.now().toString());
        info.setUnit(num);

        return info;
    }

    private int parseNumber(Matcher matcher) {
        String enNum = matcher.group("enNum");
        String zhNum = matcher.group("zhNum");
        if (enNum != null) {
            return Integer.parseInt(enNum);
        } else if (zhNum != null) {
            return zhNumParse(zhNum);
        }
        return 0;
    }

    private int getDaysByPeriod(String zhPeriod) {
        switch (zhPeriod) {
            case "周":
                return 7;
            case "月":
                return 30;
            case "年":
                return 365;
            default:
                return 1;
        }
    }

    private int zhNumParse(String zhNumStr) {
        Stack<Integer> stack = new Stack<>();
        String numStr = "一二三四五六七八九";
        String unitStr = "十百千万亿";

        for (char c : zhNumStr.toCharArray()) {
            int numIndex = numStr.indexOf(c);
            int unitIndex = unitStr.indexOf(c);
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

        return stack.stream().mapToInt(Integer::intValue).sum();
    }

    private DateConf getDateConf(Date startDate, Date endDate, String detectWord) {
        if (startDate == null || endDate == null) {
            return null;
        }

        DateConf info = new DateConf();
        info.setDateMode(DateConf.DateMode.BETWEEN);
        info.setStartDate(DATE_FORMAT.format(startDate));
        info.setEndDate(DATE_FORMAT.format(endDate));
        info.setDetectWord(detectWord);
        return info;
    }
}
