package com.tencent.supersonic.chat.parser.llm.dsl;

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
import org.apache.commons.lang3.StringUtils;

public class DSLDateHelper {

    public static String getReferenceDate(Long modelId) {
        String chatDetailDate = getChatDetailDate(modelId);
        if (StringUtils.isNotBlank(chatDetailDate)) {
            return chatDetailDate;
        }
        return DateUtils.getBeforeDate(0);
    }

    private static String getChatDetailDate(Long modelId) {
        if (Objects.isNull(modelId)) {
            return null;
        }
        ChatConfigFilter filter = new ChatConfigFilter();
        filter.setModelId(modelId);

        List<ChatConfigResp> configResps = ContextUtils.getBean(ConfigService.class).search(filter, null);
        if (CollectionUtils.isEmpty(configResps)) {
            return null;
        }
        ChatConfigResp chatConfigResp = configResps.get(0);
        if (Objects.isNull(chatConfigResp.getChatDetailConfig()) || Objects.isNull(
                chatConfigResp.getChatDetailConfig().getChatDefaultConfig())) {
            return null;
        }

        ChatDefaultConfigReq chatDefaultConfig = chatConfigResp.getChatDetailConfig().getChatDefaultConfig();
        Integer unit = chatDefaultConfig.getUnit();
        String period = chatDefaultConfig.getPeriod();
        if (Objects.nonNull(unit)) {
            DatePeriodEnum datePeriodEnum = DatePeriodEnum.get(period);
            if (Objects.isNull(datePeriodEnum)) {
                return DateUtils.getBeforeDate(unit);
            } else {
                return DateUtils.getBeforeDate(unit, datePeriodEnum);
            }
        }
        return null;
    }
}
