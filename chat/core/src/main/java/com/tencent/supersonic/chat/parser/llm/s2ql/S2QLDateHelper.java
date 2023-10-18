package com.tencent.supersonic.chat.parser.llm.s2ql;

import com.tencent.supersonic.chat.api.pojo.request.ChatConfigFilter;
import com.tencent.supersonic.chat.api.pojo.request.ChatDefaultConfigReq;
import com.tencent.supersonic.chat.api.pojo.response.ChatConfigResp;
import com.tencent.supersonic.chat.service.ConfigService;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.common.util.DatePeriodEnum;
import com.tencent.supersonic.common.util.DateUtils;
import java.util.List;
import java.util.Objects;
import org.apache.commons.collections.CollectionUtils;

public class S2QLDateHelper {

    public static String getReferenceDate(Long modelId) {
        String defaultDate = DateUtils.getBeforeDate(0);
        if (Objects.isNull(modelId)) {
            return defaultDate;
        }
        ChatConfigFilter filter = new ChatConfigFilter();
        filter.setModelId(modelId);

        List<ChatConfigResp> configResps = ContextUtils.getBean(ConfigService.class).search(filter, null);
        if (CollectionUtils.isEmpty(configResps)) {
            return defaultDate;
        }
        ChatConfigResp chatConfigResp = configResps.get(0);
        if (Objects.isNull(chatConfigResp.getChatDetailConfig()) || Objects.isNull(
                chatConfigResp.getChatDetailConfig().getChatDefaultConfig())) {
            return defaultDate;
        }

        ChatDefaultConfigReq chatDefaultConfig = chatConfigResp.getChatDetailConfig().getChatDefaultConfig();
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
