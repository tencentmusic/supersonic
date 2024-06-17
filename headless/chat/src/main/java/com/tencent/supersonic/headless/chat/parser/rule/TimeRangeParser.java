package com.tencent.supersonic.headless.chat.parser.rule;

import com.tencent.supersonic.common.pojo.Constants;
import com.tencent.supersonic.common.pojo.DateConf;
import com.tencent.supersonic.headless.chat.query.QueryManager;
import com.tencent.supersonic.headless.chat.query.SemanticQuery;
import com.tencent.supersonic.headless.chat.query.rule.RuleSemanticQuery;
import com.tencent.supersonic.headless.chat.parser.SemanticParser;
import com.tencent.supersonic.headless.chat.ChatContext;
import com.tencent.supersonic.headless.chat.QueryContext;
import com.xkzhangsan.time.nlp.TimeNLP;
import com.xkzhangsan.time.nlp.TimeNLPUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;

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
 * TimeRangeParser extracts time range specified in the user query
 * based on keyword matching.
 * Currently, it supports two kinds of expression:
 * 1. Recent unit: 近N天/周/月/年、过去N天/周/月/年
 * 2. Concrete date: 2023年11月15日、20231115
 */
@Slf4j
public class TimeRangeParser implements SemanticParser {

    private static final Pattern RECENT_PATTERN_CN = Pattern.compile(
            ".*(?<periodStr>(近|过去)((?<enNum>\\d+)|(?<zhNum>[一二三四五六七八九十百千万亿]+))个?(?<zhPeriod>[天周月年])).*");
    private static final Pattern DATE_PATTERN_NUMBER = Pattern.compile("(\\d{8})");
    private static final DateFormat DATE_FORMAT_NUMBER = new SimpleDateFormat("yyyyMMdd");
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    @Override
    public void parse(QueryContext queryContext, ChatContext chatContext) {
        String queryText = queryContext.getQueryText();
        DateConf dateConf = parseRecent(queryText);
        if (dateConf == null) {
            dateConf = parseDateNumber(queryText);
        }
        if (dateConf == null) {
            dateConf = parseDateCN(queryText);
        }

        if (dateConf != null) {
            if (queryContext.getCandidateQueries().size() > 0) {
                for (SemanticQuery query : queryContext.getCandidateQueries()) {
                    query.getParseInfo().setDateInfo(dateConf);
                    query.getParseInfo().setScore(query.getParseInfo().getScore()
                            + dateConf.getDetectWord().length());
                }
            } else if (QueryManager.containsRuleQuery(chatContext.getParseInfo().getQueryMode())) {
                RuleSemanticQuery semanticQuery = QueryManager.createRuleQuery(
                        chatContext.getParseInfo().getQueryMode());
                // inherit parse info from context
                chatContext.getParseInfo().setDateInfo(dateConf);
                chatContext.getParseInfo().setScore(chatContext.getParseInfo().getScore()
                        + dateConf.getDetectWord().length());
                semanticQuery.setParseInfo(chatContext.getParseInfo());
                queryContext.getCandidateQueries().add(semanticQuery);
            }
        }
    }

    private DateConf parseDateCN(String queryText) {
        Date startDate = null;
        Date endDate;
        String detectWord = null;

        List<TimeNLP> times = TimeNLPUtil.parse(queryText);
        if (times.size() > 0) {
            startDate = times.get(0).getTime();
            detectWord = times.get(0).getTimeExpression();
        } else {
            return null;
        }

        if (times.size() > 1) {
            endDate = times.get(1).getTime();
            detectWord += "~" + times.get(0).getTimeExpression();
        } else {
            endDate = startDate;
        }

        return getDateConf(startDate, endDate, detectWord);
    }

    private DateConf parseDateNumber(String queryText) {
        String startDate;
        String endDate = null;
        String detectWord = null;

        Matcher dateMatcher = DATE_PATTERN_NUMBER.matcher(queryText);
        if (dateMatcher.find()) {
            startDate = dateMatcher.group();
            detectWord = startDate;
        } else {
            return null;
        }

        if (dateMatcher.find()) {
            endDate = dateMatcher.group();
            detectWord += "~" + endDate;
        }

        endDate = endDate != null ? endDate : startDate;

        try {
            return getDateConf(DATE_FORMAT_NUMBER.parse(startDate), DATE_FORMAT_NUMBER.parse(endDate), detectWord);
        } catch (ParseException e) {
            return null;
        }
    }

    private DateConf parseRecent(String queryText) {
        Matcher m = RECENT_PATTERN_CN.matcher(queryText);
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
                switch (zhPeriod) {
                    case "周":
                        days = 7;
                        info.setPeriod(Constants.WEEK);
                        break;
                    case "月":
                        days = 30;
                        info.setPeriod(Constants.MONTH);
                        break;
                    case "年":
                        days = 365;
                        info.setPeriod(Constants.YEAR);
                        break;
                    default:
                        days = 1;
                        info.setPeriod(Constants.DAY);
                }
                days = days * num;
                info.setDateMode(DateConf.DateMode.RECENT);
                String detectWord = "近" + num + zhPeriod;
                if (Strings.isNotEmpty(m.group("periodStr"))) {
                    detectWord = m.group("periodStr");
                }
                info.setDetectWord(detectWord);
                info.setStartDate(LocalDate.now().minusDays(days).toString());
                info.setEndDate(LocalDate.now().minusDays(1).toString());
                info.setUnit(num);

                return info;
            }
        }

        return null;
    }

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
