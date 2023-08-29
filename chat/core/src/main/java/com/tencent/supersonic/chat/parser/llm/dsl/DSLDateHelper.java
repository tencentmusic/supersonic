package com.tencent.supersonic.chat.parser.llm.dsl;

import com.tencent.supersonic.common.util.DateUtils;

public class DSLDateHelper {

    public static String getCurrentDate(Long modelId) {
        return DateUtils.getBeforeDate(4);

    }
}
