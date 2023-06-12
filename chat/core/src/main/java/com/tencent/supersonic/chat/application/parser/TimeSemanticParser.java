package com.tencent.supersonic.chat.application.parser;

import com.tencent.supersonic.chat.api.pojo.ChatContext;
import com.tencent.supersonic.chat.api.request.QueryContextReq;
import com.tencent.supersonic.chat.api.service.SemanticParser;
import com.tencent.supersonic.common.constant.Constants;
import com.tencent.supersonic.common.pojo.DateConf;
import java.time.LocalDate;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Component;

@Component
public class TimeSemanticParser implements SemanticParser {

    public TimeSemanticParser() {
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

    private static final Pattern recentPeriodPattern = Pattern.compile(
            ".*(?<periodStr>(近|过去)((?<enNum>\\d+)|(?<zhNum>[一二三四五六七八九十百千万亿]+))个?(?<zhPeriod>[天周月年])).*");

    @Override
    public boolean parse(QueryContextReq queryContext, ChatContext chatCtx) {
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
                info.setDateMode(DateConf.DateMode.RECENT_UNITS);
                String text = "近" + num + zhPeriod;
                if (Strings.isNotEmpty(m.group("periodStr"))) {
                    text = m.group("periodStr");
                }
                info.setText(text);
                info.setStartDate(LocalDate.now().minusDays(days).toString());
                info.setUnit(days);
                queryContext.getParseInfo().setDateInfo(info);
            }
        }
        return false;
    }
}