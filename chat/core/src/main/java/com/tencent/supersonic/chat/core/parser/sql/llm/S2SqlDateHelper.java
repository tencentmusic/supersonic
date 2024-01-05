package com.tencent.supersonic.chat.core.parser.sql.llm;

import com.tencent.supersonic.chat.api.pojo.request.ChatConfigFilter;
import com.tencent.supersonic.chat.api.pojo.response.ChatConfigRichResp;
import com.tencent.supersonic.chat.api.pojo.response.ChatDefaultRichConfigResp;
import com.tencent.supersonic.chat.core.pojo.QueryContext;
import com.tencent.supersonic.common.util.DatePeriodEnum;
import com.tencent.supersonic.common.util.DateUtils;
import java.util.Objects;

public class S2SqlDateHelper {

    public static String getReferenceDate(QueryContext queryContext, Long modelId) {
        String defaultDate = DateUtils.getBeforeDate(0);
        if (Objects.isNull(modelId)) {
            return defaultDate;
        }
        ChatConfigFilter filter = new ChatConfigFilter();
        filter.setModelId(modelId);
        ChatConfigRichResp chatConfigRichResp = queryContext.getModelIdToChatRichConfig().get(modelId);

        if (Objects.isNull(chatConfigRichResp)) {
            return defaultDate;
        }
        if (Objects.isNull(chatConfigRichResp.getChatDetailRichConfig()) || Objects.isNull(
                chatConfigRichResp.getChatDetailRichConfig().getChatDefaultConfig())) {
            return defaultDate;
        }

        ChatDefaultRichConfigResp chatDefaultConfig = chatConfigRichResp.getChatDetailRichConfig()
                .getChatDefaultConfig();
        Integer unit = chatDefaultConfig.getUnit();
        String period = chatDefaultConfig.getPeriod();
        if (Objects.nonNull(unit)) {
            // If the unit is set to less than 0, then do not add relative date.
            if (unit < 0) {
                return null;
            }
            DatePeriodEnum datePeriodEnum = DatePeriodEnum.get(period);
            if (Objects.isNull(datePeriodEnum)) {
                return DateUtils.getBeforeDate(unit);
            } else {
                return DateUtils.getBeforeDate(unit, datePeriodEnum);
            }
        }
        return defaultDate;
    }

}
